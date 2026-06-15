package com.exe101.ghtk.service;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.customerAddress.entity.CustomerAddress;
import com.exe101.customerAddress.repository.ICustomerAddressRepository;
import com.exe101.ghtk.dto.*;
import com.exe101.ghtk.exception.GhtkAccessDenied;
import com.exe101.ghtk.exception.GhtkValidationException;
import com.exe101.ghtk.webhook.GhtkShipmentStatusMapper;
import com.exe101.order.entity.CustomerOrder;
import com.exe101.order.entity.OrderItem;
import com.exe101.order.entity.OrderStatus;
import com.exe101.order.exception.OrderNotFound;
import com.exe101.order.repository.IOrderItemRepository;
import com.exe101.order.repository.IOrderRepository;
import com.exe101.order.service.OrderService;
import com.exe101.product.entity.Product;
import com.exe101.product.repository.IProductRepository;
import com.exe101.shipping.entity.ShipmentStatus;
import com.exe101.shipping.entity.ShopOrderShipment;
import com.exe101.shipping.repository.IShopOrderShipmentRepository;
import com.exe101.shop.entity.Shop;
import com.exe101.shop.repository.IShopRepository;
import com.exe101.shopGhtkConfig.entity.ShopGhtkConfig;
import com.exe101.shopGhtkConfig.exception.ShopGhtkConfigNotFound;
import com.exe101.shopGhtkConfig.repository.IShopGhtkConfigRepository;
import com.exe101.shopGhtkConfig.service.GhtkConfigCryptoService;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import com.exe101.userAddress.entity.UserAddress;
import com.exe101.userAddress.repository.IUserAddressRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GhtkOrderService {

    private static final int MAX_GHTK_NOTE_LENGTH = 120;
    private static final Duration GHTK_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration GHTK_READ_TIMEOUT = Duration.ofSeconds(20);
    private static final List<String> GHTK_PICK_OPTIONS = List.of("cod", "post");
    private static final List<String> GHTK_TRANSPORTS = List.of("road", "fly");
    private static final List<Integer> GHTK_CANCELABLE_STATUS_IDS = List.of(1, 2, 12);
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter GHTK_PICK_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Logger log = LoggerFactory.getLogger(GhtkOrderService.class);

    private final IOrderRepository orderRepository;
    private final IOrderItemRepository orderItemRepository;
    private final IProductRepository productRepository;
    private final IShopRepository shopRepository;
    private final ICustomerAddressRepository customerAddressRepository;
    private final IUserAddressRepository userAddressRepository;
    private final IShopGhtkConfigRepository shopGhtkConfigRepository;
    private final IShopMemberRepository shopMemberRepository;
    private final IShopOrderShipmentRepository shipmentRepository;
    private final GhtkConfigCryptoService cryptoService;
    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    private final RestTemplate restTemplate = buildRestTemplate();

    @Value("${ghtk.submit-order-url}")
    private String submitOrderUrl;

    @Value("${ghtk.fee-url}")
    private String feeUrl;

    @Value("${ghtk.cancel-shipment-url:https://services.giaohangtietkiem.vn/services/shipment/cancel}")
    private String cancelShipmentUrl;

    @Transactional
    public GhtkSubmitOrderResponse submitOrder(Long shopId, Long orderId, GhtkSubmitOrderRequest request) {
        assertCanSubmitOrder(shopId);

        CustomerOrder order = orderRepository.findByIdAndShopIdForUpdate(orderId, shopId)
                .orElseThrow(() -> new OrderNotFound("OrderNotFound", "Không tìm thấy đơn hàng"));
        applyCustomerAddressSnapshot(order);
        orderRepository.save(order);
        validateOrder(order);

        ShopGhtkConfig config = shopGhtkConfigRepository.findByShopId(shopId)
                .orElseThrow(() -> new ShopGhtkConfigNotFound(
                        "ShopGhtkConfigNotFound",
                        "Chưa có cấu hình GHTK cho shop"));
        validateConfig(config);
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new GhtkValidationException(
                        "GhtkShopNotFound",
                        "Không tìm thấy shop để lấy thông tin điểm lấy hàng"));
        validateShopPickupInfo(shop);

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        if (items.isEmpty()) {
            throw new GhtkValidationException(
                    "GhtkOrderItemsRequired",
                    "Đơn hàng cần có sản phẩm để đăng GHTK");
        }
        validateOrderItems(shopId, items);
        Map<Long, Product> productsById = loadProductsById(shopId, items);

        String apiToken = cryptoService.decrypt(config.getEncryptedApiToken());
        Map<String, Object> body = buildSubmitBody(order, items, productsById, shop, config, request);
        logSubmitPayload(body);
        GhtkSubmitOrderResponse response = callGhtk(apiToken, resolveClientSource(config), body);
        markSubmittedIfAccepted(order, response);
        return response;
    }

    public GhtkFeeResponse calculateFee(Long shopId, GhtkFeeRequest request) {
        ShopGhtkConfig config = shopGhtkConfigRepository.findByShopId(shopId)
                .orElseThrow(() -> new ShopGhtkConfigNotFound(
                        "ShopGhtkConfigNotFound",
                        "Chưa có cấu hình GHTK cho shop"));
        validateFeeConfig(config);

        UserAddress address = resolveUserAddressForCurrentUser(request.getUserAddressId());
        String apiToken = cryptoService.decrypt(config.getEncryptedApiToken());
        String transport = resolveTransport(request.getTransport());
        String url = buildFeeUrl(config, address, request, transport);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", apiToken);
        String clientSource = resolveClientSource(config);
        if (!isBlank(clientSource)) {
            headers.set("X-Client-Source", clientSource);
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            String rawResponseBody = response.getBody();
            if (rawResponseBody == null || rawResponseBody.isBlank()) {
                throw new GhtkValidationException(
                        "GhtkEmptyFeeResponse",
                        "GHTK không trả dữ liệu phí vận chuyển");
            }

            return objectMapper.readValue(rawResponseBody, GhtkFeeResponse.class);
        } catch (HttpStatusCodeException ex) {
            throw new GhtkValidationException(
                    "GhtkFeeFailed",
                    resolveGhtkFeeErrorMessage(ex));
        } catch (RestClientException ex) {
            throw new GhtkValidationException(
                    "GhtkFeeFailed",
                    "Không thể đọc phản hồi từ GHTK khi tính phí");
        } catch (Exception ex) {
            throw new GhtkValidationException(
                    "GhtkFeeFailed",
                    "Phản hồi tính phí từ GHTK không đúng định dạng");
        }
    }

    @Transactional
    public GhtkCancelShipmentResponse cancelShipment(Long shopId, Long orderId) {
        assertCanSubmitOrder(shopId);

        CustomerOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFound("OrderNotFound", "Không tìm thấy đơn hàng"));
        assertOrderBelongsToShop(order, shopId);

        ShopOrderShipment existingShipment = shipmentRepository.findByOrderIdForUpdate(order.getId()).orElse(null);

        if (!hasGhtkShipmentIdentifier(existingShipment) && canCancelOrderInternally(order.getStatus())) {
            order.setStatus(OrderStatus.CANCELLED);
            CustomerOrder saved = orderRepository.save(order);
            return new GhtkCancelShipmentResponse(
                    true,
                    "Hủy đơn hàng thành công",
                    saved.getId(),
                    null,
                    null,
                    null,
                    saved.getStatus().name()
            );
        }

        ShopOrderShipment shipment = shipmentRepository.findByOrderIdForUpdate(order.getId())
                .orElseThrow(() -> new GhtkValidationException(
                        "GhtkShipmentNotFound",
                        "Đơn hàng chưa tạo vận đơn GHTK"));
        validateShipmentBelongsToOrder(shipment, order);
        validateShipmentCanBeCanceled(shipment);

        ShopGhtkConfig config = shopGhtkConfigRepository.findByShopId(shopId)
                .orElseThrow(() -> new ShopGhtkConfigNotFound(
                        "ShopGhtkConfigNotFound",
                        "Chưa có cấu hình GHTK cho shop"));
        validateCancelConfig(config);

        String apiToken = cryptoService.decrypt(config.getEncryptedApiToken());
        if (isBlank(apiToken)) {
            throw new GhtkValidationException(
                    "GhtkApiTokenRequired",
                    "Shop chưa cấu hình GHTK token");
        }

        String clientSource = resolveClientSource(config);
        String cancelUrl = buildCancelShipmentUrl(shipment);
        GhtkCancelApiResponse ghtkResponse = callGhtkCancel(
                cancelUrl,
                apiToken,
                clientSource,
                shipment
        );

        if (!ghtkResponse.isSuccess()) {
            String message = !isBlank(ghtkResponse.getMessage())
                    ? ghtkResponse.getMessage()
                    : "GHTK từ chối hủy vận đơn";
            log.warn(
                    "GHTK cancel returned success=false: orderId={}, labelId={}, partnerId={}, message={}, logId={}",
                    order.getId(),
                    shipment.getLabelId(),
                    shipment.getPartnerId(),
                    ghtkResponse.getMessage(),
                    ghtkResponse.getLogId()
            );
            throw new GhtkValidationException("GhtkCancelRejected", message);
        }

        shipment.setStatus(ShipmentStatus.CANCELED);
        shipment.setGhtkStatusId(-1);
        shipment.setLastActionTime(OffsetDateTime.now());
        shipment.setReason(trim(ghtkResponse.getMessage()));
        shipment.setReasonCode(trim(ghtkResponse.getLogId()));
        ShopOrderShipment savedShipment = shipmentRepository.save(shipment);

        order.setStatus(OrderStatus.CANCELLED);
        CustomerOrder savedOrder = orderRepository.save(order);

        return new GhtkCancelShipmentResponse(
                true,
                "Hủy vận đơn GHTK thành công",
                savedOrder.getId(),
                savedShipment.getLabelId(),
                savedShipment.getPartnerId(),
                savedShipment.getStatus().name(),
                savedOrder.getStatus().name()
        );
    }

    private GhtkSubmitOrderResponse callGhtk(
            String apiToken,
            String clientSource,
            Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", apiToken);
        headers.set("Accept", "*/*");
        headers.set("User-Agent", "PostmanRuntime/7.53.0");
        if (!isBlank(clientSource)) {
            headers.set("X-Client-Source", clientSource);
        }

        try {
            ResponseEntity<GhtkSubmitOrderResponse> response = restTemplate.exchange(
                    submitOrderUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    GhtkSubmitOrderResponse.class);
            if (response.getBody() == null) {
                throw new GhtkValidationException(
                        "GhtkEmptyResponse",
                        "GHTK không trả dữ liệu đăng đơn");
            }
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            log.warn(
                    "GHTK submit failed: status={}, responseBody={}",
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString()
            );
            throw new GhtkValidationException(
                    "GhtkSubmitFailed",
                    resolveGhtkErrorMessage(ex));
        }
    }

    private Map<String, Object> buildSubmitBody(
            CustomerOrder order,
            List<OrderItem> items,
            Map<Long, Product> productsById,
            Shop shop,
            ShopGhtkConfig config,
            GhtkSubmitOrderRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("products", items.stream()
                .map(item -> buildProductPayload(item, productsById.get(item.getProductId())))
                .toList());
        body.put("order", buildOrderPayload(order, shop, config, request));
        return body;
    }

    private void logSubmitPayload(Map<String, Object> body) {
        try {
            log.info("GHTK submit payload: {}", objectMapper.writeValueAsString(body));
        } catch (Exception ex) {
            log.info("GHTK submit payload could not be serialized", ex);
        }
    }

    private GhtkCancelApiResponse callGhtkCancel(
            String url,
            String apiToken,
            String clientSource,
            ShopOrderShipment shipment
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", apiToken);
        headers.set("Accept", "*/*");
        if (!isBlank(clientSource)) {
            headers.set("X-Client-Source", clientSource);
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(headers),
                    String.class);
            String rawResponseBody = response.getBody();
            log.info(
                    "GHTK cancel response: orderId={}, labelId={}, partnerId={}, status={}, body={}",
                    shipment.getOrderId(),
                    shipment.getLabelId(),
                    shipment.getPartnerId(),
                    response.getStatusCode(),
                    rawResponseBody
            );
            if (rawResponseBody == null || rawResponseBody.isBlank()) {
                throw new GhtkValidationException(
                        "GhtkEmptyCancelResponse",
                        "GHTK không trả dữ liệu hủy vận đơn");
            }
            return objectMapper.readValue(rawResponseBody, GhtkCancelApiResponse.class);
        } catch (HttpStatusCodeException ex) {
            log.warn(
                    "GHTK cancel failed: orderId={}, labelId={}, partnerId={}, status={}, responseBody={}",
                    shipment.getOrderId(),
                    shipment.getLabelId(),
                    shipment.getPartnerId(),
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString()
            );
            throw new GhtkValidationException(
                    "GhtkCancelFailed",
                    resolveGhtkCancelErrorMessage(ex));
        } catch (ResourceAccessException ex) {
            log.warn(
                    "GHTK cancel timeout or connection error: orderId={}, labelId={}, partnerId={}",
                    shipment.getOrderId(),
                    shipment.getLabelId(),
                    shipment.getPartnerId(),
                    ex
            );
            throw new GhtkValidationException(
                    "GhtkCancelTimeout",
                    "Không thể kết nối GHTK để hủy vận đơn, vui lòng thử lại sau");
        } catch (RestClientException ex) {
            log.warn(
                    "GHTK cancel request error: orderId={}, labelId={}, partnerId={}",
                    shipment.getOrderId(),
                    shipment.getLabelId(),
                    shipment.getPartnerId(),
                    ex
            );
            throw new GhtkValidationException(
                    "GhtkCancelFailed",
                    "Không thể gọi GHTK để hủy vận đơn");
        } catch (Exception ex) {
            log.warn(
                    "GHTK cancel parse error: orderId={}, labelId={}, partnerId={}",
                    shipment.getOrderId(),
                    shipment.getLabelId(),
                    shipment.getPartnerId(),
                    ex
            );
            throw new GhtkValidationException(
                    "GhtkCancelInvalidResponse",
                    "Phản hồi hủy vận đơn từ GHTK không đúng định dạng");
        }
    }

    private Map<String, Object> buildProductPayload(OrderItem item, Product product) {
        if (product == null) {
            throw new GhtkValidationException(
                    "GhtkProductNotFound",
                    "Không tìm thấy sản phẩm #" + item.getProductId() + " của đơn hàng");
        }
        if (isBlank(product.getName())) {
            throw new GhtkValidationException(
                    "GhtkProductNameRequired",
                    "Sản phẩm #" + item.getProductId() + " chưa có tên hợp lệ");
        }
        if (product.getWeightKg() == null || product.getWeightKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new GhtkValidationException(
                    "GhtkProductWeightRequired",
                    "Sản phẩm " + product.getName() + " chưa có khối lượng hợp lệ");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", trim(product.getName()));
        payload.put("weight", toGhtkSubmitWeightKg(product.getWeightKg()));
        payload.put("quantity", item.getQty());
        payload.put("product_code",
                !isBlank(product.getSku()) ? trim(product.getSku()) : String.valueOf(product.getId()));
        payload.put("price", item.getUnitPrice());
        return payload;
    }

    private BigDecimal toGhtkSubmitWeightKg(BigDecimal weightGram) {
        return weightGram.divide(new BigDecimal("1000"));
    }

    private Map<String, Object> buildOrderPayload(
            CustomerOrder order,
            Shop shop,
            ShopGhtkConfig config,
            GhtkSubmitOrderRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", resolvePartnerOrderId(order));
        payload.put("pick_name", trim(shop.getName()));
        payload.put("pick_tel", trim(shop.getPhone()));
        payload.put("pick_address", trim(shop.getAddressText()));
        payload.put("pick_province", trim(config.getPickProvince()));
        payload.put("pick_district", trim(config.getPickDistrict()));
        if (!isBlank(config.getPickWard())) {
            payload.put("pick_ward", trim(config.getPickWard()));
        }
        payload.put("pick_option", trim(config.getPickOption()));
        payload.put("transport", trim(config.getTransport()));
        payload.put("pick_date", resolvePickDate(request));
        payload.put("name", trim(order.getReceiverName()));
        payload.put("tel", trim(order.getReceiverPhone()));
        payload.put("address", trim(order.getShippingAddress()));
        payload.put("province", trim(order.getShippingProvince()));
        payload.put("district", trim(order.getShippingDistrict()));
        payload.put("ward", trim(order.getShippingWard()));
        if (!isBlank(order.getShippingStreet())) {
            payload.put("street", trim(order.getShippingStreet()));
        } else {
            payload.put("hamlet", trim(order.getShippingHamlet()));
        }
        int isFreeship = resolveIsFreeship(request);
        payload.put("pick_money", resolveGhtkPickMoney(order, isFreeship));
        payload.put("is_freeship", isFreeship);
        payload.put("value", valueOrZero(order.getSubtotalAmount()) > 0
                ? valueOrZero(order.getSubtotalAmount())
                : valueOrZero(order.getTotalAmount()));

        String note = resolveNote(order, request);
        if (!isBlank(note)) {
            payload.put("note", note);
        }
        return payload;
    }

    private String resolvePickDate(GhtkSubmitOrderRequest request) {
        if (request == null || request.getPickDate() == null) {
            throw new GhtkValidationException(
                    "GhtkPickDateRequired",
                    "Ngày lấy hàng GHTK không được để trống");
        }

        LocalDate today = LocalDate.now(VIETNAM_ZONE);
        LocalDate pickDate = request.getPickDate();
        if (pickDate.isBefore(today)) {
            throw new GhtkValidationException(
                    "GhtkPickDateInvalid",
                    "Ngày lấy hàng GHTK không được nhỏ hơn ngày hiện tại");
        }

        return pickDate.format(GHTK_PICK_DATE_FORMATTER);
    }

    private int resolveIsFreeship(GhtkSubmitOrderRequest request) {
        if (request == null || request.getIsFreeship() == null) {
            return 0;
        }
        if (request.getIsFreeship() != 0 && request.getIsFreeship() != 1) {
            throw new GhtkValidationException(
                    "GhtkIsFreeshipInvalid",
                    "isFreeship chỉ được là 0 hoặc 1");
        }
        return request.getIsFreeship();
    }

    private long resolveGhtkPickMoney(CustomerOrder order, int isFreeship) {
        if (isFreeship == 1) {
            return valueOrZero(order.getTotalAmount());
        }

        long subtotal = valueOrZero(order.getSubtotalAmount());
        long discountAmount = valueOrZero(order.getDiscountAmount());
        long amount = subtotal - discountAmount;
        if (amount > 0) {
            return amount;
        }
        return subtotal > 0 ? subtotal : valueOrZero(order.getTotalAmount());
    }

    private String resolveNote(CustomerOrder order, GhtkSubmitOrderRequest request) {
        String note = request != null && !isBlank(request.getNote()) ? trim(request.getNote()) : trim(order.getNote());
        if (note != null && note.length() > MAX_GHTK_NOTE_LENGTH) {
            throw new GhtkValidationException(
                    "GhtkNoteTooLong",
                    "Ghi chú GHTK không được vượt quá 120 ký tự");
        }
        return note;
    }

private void markSubmittedIfAccepted(CustomerOrder order, GhtkSubmitOrderResponse response) {
    if (response.isSuccess()) {
        order.setStatus(OrderStatus.WAITING_GHTK_PICKUP);

        CustomerOrder saved = orderRepository.save(order);

        upsertShipmentFromSubmitResponse(saved, response);

        try {
            orderService.publishOrderStatusUpdatedNotification(saved);
        } catch (Exception e) {
            log.error("Failed to publish order status update notification", e);
        }
    }
}

    private void upsertShipmentFromSubmitResponse(CustomerOrder order, GhtkSubmitOrderResponse response) {
        JsonNode responseOrder = response.getOrder();
        String partnerId = firstNonBlank(
                text(responseOrder, "partner_id"),
                resolvePartnerOrderId(order)
        );

        ShopOrderShipment shipment = shipmentRepository.findByOrderIdForUpdate(order.getId())
                .orElseGet(ShopOrderShipment::new);
        shipment.setShopId(order.getShopId());
        shipment.setOrderId(order.getId());
        shipment.setCarrier(ShopOrderShipment.CARRIER_GHTK);
        shipment.setPartnerId(partnerId);
        shipment.setLabelId(text(responseOrder, "label"));
        shipment.setTrackingId(text(responseOrder, "tracking_id"));
        shipment.setActualShippingFee(parseLong(text(responseOrder, "fee")));

        Integer statusId = parseInteger(text(responseOrder, "status_id"));
        ShipmentStatus status = GhtkShipmentStatusMapper.toShipmentStatus(statusId);
        shipment.setGhtkStatusId(statusId);
        shipment.setStatus(status != null ? status : ShipmentStatus.ACCEPTED);

        shipmentRepository.save(shipment);
    }

    private void applyCustomerAddressSnapshot(CustomerOrder order) {
        if (order.getCustomerAddressId() == null && order.getCustomerId() == null) {
            return;
        }
        CustomerAddress address = resolveCustomerAddress(order);
        order.setCustomerAddressId(address.getId());
        order.setReceiverName(address.getName());
        order.setReceiverPhone(address.getTel());
        order.setShippingAddress(address.getAddress());
        order.setShippingProvince(address.getProvince());
        order.setShippingDistrict(address.getDistrict());
        order.setShippingWard(address.getWard());
        order.setShippingStreet(null);
        order.setShippingHamlet(address.getHamlet());
    }

    private CustomerAddress resolveCustomerAddress(CustomerOrder order) {
        if (order.getCustomerAddressId() != null) {
            CustomerAddress address = customerAddressRepository.findById(order.getCustomerAddressId())
                    .orElseThrow(() -> new GhtkValidationException(
                            "GhtkCustomerAddressNotFound",
                            "Không tìm thấy địa chỉ nhận hàng của khách hàng"));
            validateCustomerAddressMatchesOrderCustomer(order, address);
            return address;
        }

        if (order.getCustomerId() == null) {
            throw new GhtkValidationException(
                    "GhtkOrderCustomerRequired",
                    "Đơn hàng cần gắn khách hàng để lấy địa chỉ nhận hàng");
        }

        return customerAddressRepository.findFirstByCustomerIdOrderByDefaultAddressDescIdDesc(order.getCustomerId())
                .orElseThrow(() -> new GhtkValidationException(
                        "GhtkCustomerAddressRequired",
                        "Khách hàng của đơn hàng chưa có địa chỉ nhận hàng"));
    }

    private void validateCustomerAddressMatchesOrderCustomer(CustomerOrder order, CustomerAddress address) {
        if (order.getCustomerId() == null) {
            return;
        }

        if (!Objects.equals(order.getCustomerId(), address.getCustomerId())) {
            throw new GhtkValidationException(
                    "GhtkCustomerAddressMismatch",
                    "Địa chỉ nhận hàng không thuộc khách hàng trong đơn");
        }
    }

    private void validateOrder(CustomerOrder order) {
        if (order.getStatus() == null) {
            throw new GhtkValidationException(
                    "GhtkOrderStatusRequired",
                    "Đơn hàng chưa có trạng thái hợp lệ");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new GhtkValidationException(
                    "GhtkOrderCancelled",
                    "Không thể đăng GHTK cho đơn hàng đã hủy");
        }
        if (order.getStatus() != OrderStatus.PACKING) {
            throw new GhtkValidationException(
                    "GhtkOrderNotPacking",
                    "Chỉ có thể gửi GHTK cho đơn hàng đang đóng gói");
        }
        validateOrderAmounts(order);

        if (isBlank(order.getReceiverName())
                || isBlank(order.getReceiverPhone())
                || isBlank(order.getShippingAddress())
                || isBlank(order.getShippingProvince())
                || isBlank(order.getShippingDistrict())
                || isBlank(order.getShippingWard())) {
            throw new GhtkValidationException(
                    "GhtkReceiverInfoRequired",
                    "Đơn hàng chưa đủ thông tin người nhận và địa chỉ giao hàng");
        }
        if (isBlank(order.getShippingStreet()) && isBlank(order.getShippingHamlet())) {
            throw new GhtkValidationException(
                    "GhtkStreetOrHamletRequired",
                    "Đơn hàng cần có street hoặc hamlet để đăng GHTK");
        }
    }

    private void assertOrderBelongsToShop(CustomerOrder order, Long shopId) {
        if (!Objects.equals(order.getShopId(), shopId)) {
            throw new GhtkAccessDenied(
                    "GhtkOrderAccessDenied",
                    "Đơn hàng không thuộc shop hiện tại");
        }
    }

    private void validateShipmentBelongsToOrder(ShopOrderShipment shipment, CustomerOrder order) {
        if (!Objects.equals(shipment.getShopId(), order.getShopId())
                || !Objects.equals(shipment.getOrderId(), order.getId())) {
            throw new GhtkValidationException(
                    "GhtkShipmentOrderMismatch",
                    "Vận đơn GHTK không khớp với đơn hàng hiện tại");
        }
    }

    private void validateShipmentCanBeCanceled(ShopOrderShipment shipment) {
        if (isBlank(shipment.getLabelId()) && isBlank(shipment.getPartnerId())) {
            throw new GhtkValidationException(
                    "GhtkShipmentIdentifierRequired",
                    "Đơn hàng chưa tạo vận đơn GHTK");
        }
        if (shipment.getStatus() == ShipmentStatus.CANCELED || Objects.equals(shipment.getGhtkStatusId(), -1)) {
            throw new GhtkValidationException(
                    "GhtkShipmentAlreadyCanceled",
                    "Vận đơn GHTK đã được hủy");
        }
        Integer statusId = shipment.getGhtkStatusId();
        if (statusId == null || !GHTK_CANCELABLE_STATUS_IDS.contains(statusId)) {
            throw new GhtkValidationException(
                    "GhtkShipmentCancelNotAllowed",
                    "Đơn đã được lấy hàng hoặc đang giao, không thể hủy vận đơn GHTK.");
        }
    }

    private void validateOrderAmounts(CustomerOrder order) {
        long subtotal = valueOrZero(order.getSubtotalAmount());
        long shippingFee = valueOrZero(order.getShippingFee());
        long discountAmount = valueOrZero(order.getDiscountAmount());
        long total = valueOrZero(order.getTotalAmount());

        if (subtotal < 0 || shippingFee < 0 || discountAmount < 0 || total < 0) {
            throw new GhtkValidationException(
                    "GhtkOrderAmountInvalid",
                    "Số tiền của đơn hàng không hợp lệ để đăng GHTK");
        }

        long expectedTotal = subtotal + shippingFee - discountAmount;
        if (total != expectedTotal) {
            throw new GhtkValidationException(
                    "GhtkOrderTotalMismatch",
                    "Tổng tiền đơn hàng không khớp với tạm tính, phí giao hàng và giảm giá");
        }
    }

    private void validateOrderItems(Long shopId, List<OrderItem> items) {
        for (OrderItem item : items) {
            if (!Objects.equals(item.getShopId(), shopId)) {
                throw new GhtkValidationException(
                        "GhtkOrderItemShopMismatch",
                        "Sản phẩm trong đơn hàng không thuộc shop đang gửi GHTK");
            }
            if (item.getProductId() == null) {
                throw new GhtkValidationException(
                        "GhtkOrderItemProductRequired",
                        "Sản phẩm trong đơn hàng không được để trống");
            }
            if (item.getQty() == null || item.getQty() <= 0) {
                throw new GhtkValidationException(
                        "GhtkOrderItemQtyInvalid",
                        "Số lượng sản phẩm trong đơn hàng phải lớn hơn 0");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice() < 0) {
                throw new GhtkValidationException(
                        "GhtkOrderItemPriceInvalid",
                        "Đơn giá sản phẩm trong đơn hàng không hợp lệ");
            }
            long expectedAmount = item.getUnitPrice() * item.getQty();
            if (item.getAmount() == null || item.getAmount() != expectedAmount) {
                throw new GhtkValidationException(
                        "GhtkOrderItemAmountMismatch",
                        "Thành tiền sản phẩm trong đơn hàng không khớp với số lượng và đơn giá");
            }
        }
    }

    private void validateConfig(ShopGhtkConfig config) {
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new GhtkValidationException(
                    "GhtkConfigDisabled",
                    "Cấu hình GHTK của shop đang tắt");
        }
        if (isBlank(config.getEncryptedApiToken())
                || isBlank(config.getPickProvince())
                || isBlank(config.getPickDistrict())) {
            throw new GhtkValidationException(
                    "GhtkConfigInvalid",
                    "Cấu hình GHTK của shop chưa đầy đủ");
        }
        if (!GHTK_PICK_OPTIONS.contains(trim(config.getPickOption()))) {
            throw new GhtkValidationException(
                    "GhtkPickOptionInvalid",
                    "Hình thức lấy hàng GHTK chỉ được là cod hoặc post");
        }
        if (!GHTK_TRANSPORTS.contains(trim(config.getTransport()))) {
            throw new GhtkValidationException(
                    "GhtkTransportInvalid",
                    "Phương thức vận chuyển GHTK chỉ được là road hoặc fly");
        }
    }

    private void validateShopPickupInfo(Shop shop) {
        if (isBlank(shop.getName()) || isBlank(shop.getPhone()) || isBlank(shop.getAddressText())) {
            throw new GhtkValidationException(
                    "GhtkShopPickupInfoRequired",
                    "Hồ sơ shop cần có tên, số điện thoại và địa chỉ để đăng đơn GHTK");
        }
    }

    private void validateFeeConfig(ShopGhtkConfig config) {
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new GhtkValidationException(
                    "GhtkConfigDisabled",
                    "Cấu hình GHTK của shop đang tắt");
        }
        if (isBlank(config.getEncryptedApiToken())
                || isBlank(config.getPickAddress())
                || isBlank(config.getPickProvince())
                || isBlank(config.getPickDistrict())) {
            throw new GhtkValidationException(
                    "GhtkConfigInvalid",
                    "Cấu hình GHTK của shop chưa đủ thông tin lấy hàng để tính phí");
        }
    }

    private void validateCancelConfig(ShopGhtkConfig config) {
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new GhtkValidationException(
                    "GhtkConfigDisabled",
                    "Cấu hình GHTK của shop đang tắt");
        }
        if (isBlank(config.getEncryptedApiToken())) {
            throw new GhtkValidationException(
                    "GhtkApiTokenRequired",
                    "Shop chưa cấu hình GHTK token");
        }
    }

    private Map<Long, Product> loadProductsById(Long shopId, List<OrderItem> items) {
        List<Long> productIds = items.stream()
                .map(OrderItem::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return productRepository.findAllById(productIds).stream()
                .filter(product -> Objects.equals(product.getShopId(), shopId))
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private String resolveClientSource(ShopGhtkConfig config) {
        if (!isBlank(config.getClientSource())) {
            return config.getClientSource().trim();
        }
        return null;
    }

    private String resolvePartnerOrderId(CustomerOrder order) {
        return "ORD-" + order.getId();
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        return !isBlank(text) ? trim(text) : null;
    }

    private Integer parseInteger(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstNonBlank(String first, String fallback) {
        return !isBlank(first) ? trim(first) : trim(fallback);
    }

    private String resolveGhtkErrorMessage(HttpStatusCodeException ex) {
        try {
            GhtkSubmitOrderResponse response = objectMapper.readValue(
                    ex.getResponseBodyAsString(),
                    GhtkSubmitOrderResponse.class);
            if (!isBlank(response.getMessage())) {
                return response.getMessage();
            }
        } catch (Exception ignored) {
            // Fall back to HTTP status below.
        }
        return "GHTK từ chối đăng đơn: " + ex.getStatusCode();
    }

    private String resolveGhtkFeeErrorMessage(HttpStatusCodeException ex) {
        try {
            String message = objectMapper.readTree(ex.getResponseBodyAsString()).path("message").asText(null);
            if (!isBlank(message)) {
                return message;
            }
        } catch (Exception ignored) {
            // Fall back to HTTP status below.
        }
        return "GHTK từ chối tính phí: " + ex.getStatusCode();
    }

    private String resolveGhtkCancelErrorMessage(HttpStatusCodeException ex) {
        try {
            String message = objectMapper.readTree(ex.getResponseBodyAsString()).path("message").asText(null);
            if (!isBlank(message)) {
                return message;
            }
        } catch (Exception ignored) {
            // Fall back to HTTP status below.
        }
        return "GHTK từ chối hủy vận đơn: " + ex.getStatusCode();
    }

    private String buildCancelShipmentUrl(ShopOrderShipment shipment) {
        String baseUrl = cancelShipmentUrl != null ? cancelShipmentUrl.trim() : "";
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String identifier = !isBlank(shipment.getLabelId())
                ? trim(shipment.getLabelId())
                : "partner_id:" + trim(shipment.getPartnerId());
        return baseUrl + "/" + identifier;
    }

    private String buildFeeUrl(
            ShopGhtkConfig config,
            UserAddress address,
            GhtkFeeRequest request,
            String transport) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(feeUrl)
                .queryParam("pick_province", trim(config.getPickProvince()))
                .queryParam("pick_district", trim(config.getPickDistrict()))
                .queryParam("pick_address", trim(config.getPickAddress()))
                .queryParam("province", trim(address.getProvince()))
                .queryParam("district", trim(address.getDistrict()))
                .queryParam("address", trim(address.getAddress()))
                .queryParam("weight", request.getWeight())
                .queryParam("value", request.getValue())
                .queryParam("transport", transport);

        return builder.build(false).toUriString();
    }

    private UserAddress resolveUserAddressForCurrentUser(Long userAddressId) {
        Long userId = getCurrentUserId();
        return userAddressRepository.findByIdAndUserId(userAddressId, userId)
                .orElseThrow(() -> new GhtkValidationException(
                        "GhtkUserAddressNotFound",
                        "Không tìm thấy địa chỉ nhận hàng của người dùng"));
    }

    private String resolveTransport(String transport) {
        String normalized = trim(transport);
        if (!GHTK_TRANSPORTS.contains(normalized)) {
            throw new GhtkValidationException(
                    "GhtkTransportInvalid",
                    "Phương thức vận chuyển GHTK chỉ được là road hoặc fly");
        }
        return normalized;
    }

    private void assertCanSubmitOrder(Long shopId) {
        Long userId = getCurrentUserId();
        boolean allowed = shopMemberRepository.existsByShopIdAndUserIdAndStatus(
                shopId,
                userId,
                MemberStatus.ACTIVE);

        if (!allowed) {
            throw new GhtkAccessDenied(
                    "GhtkAccessDenied",
                    "Bạn không có quyền đăng đơn GHTK cho shop này");
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new GhtkAccessDenied(
                    "GhtkAccessDenied",
                    "Bạn cần đăng nhập để đăng đơn GHTK");
        }
        return userPrincipal.getUser().getId();
    }

    private boolean hasGhtkShipmentIdentifier(ShopOrderShipment shipment) {
        return shipment != null && (!isBlank(shipment.getLabelId()) || !isBlank(shipment.getPartnerId()));
    }

    private boolean canCancelOrderInternally(OrderStatus status) {
        return status == OrderStatus.PENDING
                || status == OrderStatus.CONFIRMED
                || status == OrderStatus.PACKING;
    }

    private long valueOrZero(Long value) {
        return value != null ? value : 0L;
    }

    private String trim(String value) {
        return value != null ? value.trim() : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) GHTK_CONNECT_TIMEOUT.toMillis());
        requestFactory.setReadTimeout((int) GHTK_READ_TIMEOUT.toMillis());
        return new RestTemplate(requestFactory);
    }

}
