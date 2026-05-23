package com.exe101.order.service;

import com.exe101.common.IService;
import com.exe101.common.ScrollResponse;
import com.exe101.customer.entity.Customer;
import com.exe101.customer.repository.ICustomerRepository;
import com.exe101.customerAddress.entity.CustomerAddress;
import com.exe101.customerAddress.repository.ICustomerAddressRepository;
import com.exe101.notification.dto.NotificationTargetType;
import com.exe101.notification.dto.NotificationType;
import com.exe101.notification.service.NotificationService;
import com.exe101.order.dto.OrderDTO;
import com.exe101.order.dto.OrderItemDTO;
import com.exe101.order.dto.OrderListItemDTO;
import com.exe101.order.entity.CustomerOrder;
import com.exe101.order.entity.OrderItem;
import com.exe101.order.entity.OrderSource;
import com.exe101.order.entity.OrderStatus;
import com.exe101.order.exception.OrderNotFound;
import com.exe101.order.exception.OrderValidationException;
import com.exe101.order.mapper.OrderMapper;
import com.exe101.order.repository.IOrderItemRepository;
import com.exe101.order.repository.IOrderRepository;
import com.exe101.product.entity.Product;
import com.exe101.product.repository.IProductRepository;
import com.exe101.user.entity.User;
import com.exe101.user.entity.UserStatus;
import com.exe101.user.repository.IUserRepository;
import com.exe101.userAddress.entity.UserAddress;
import com.exe101.userAddress.repository.IUserAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService implements IService<CustomerOrder, OrderDTO, Long> {

    private static final int MAX_SCROLL_SIZE = 50;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final IOrderRepository orderRepository;
    private final IOrderItemRepository orderItemRepository;
    private final ICustomerRepository customerRepository;
    private final IProductRepository productRepository;
    private final ICustomerAddressRepository customerAddressRepository;
    private final NotificationService notificationService;
    private final IUserRepository userRepository;
    private final IUserAddressRepository userAddressRepository;

    @Override
    public List<OrderDTO> getAll() {
        return toDTOs(orderRepository.findAll());
    }

    public ScrollResponse<OrderListItemDTO> getAllForScroll(
            Long shopId,
            Long userId,
            Long customerId,
            OrderStatus status,
            OrderSource source,
            LocalDate createdDate,
            Long cursor,
            int size
    ) {
        int normalizedSize = Math.min(Math.max(size, 1), MAX_SCROLL_SIZE);
        Long normalizedCursor = cursor != null && cursor > 0 ? cursor : null;
        OffsetDateTime createdFrom = toStartOfDay(createdDate);
        OffsetDateTime createdTo = toStartOfNextDay(createdDate);
        CustomerOrder cursorOrder = resolveCursorOrder(shopId, normalizedCursor);

        List<CustomerOrder> orders = orderRepository.findForScroll(
                shopId,
                userId,
                customerId,
                status,
                source,
                createdFrom,
                createdTo,
                cursorOrder != null ? cursorOrder.getId() : null,
                cursorOrder != null ? toSortPriority(cursorOrder.getStatus()) : null,
                cursorOrder != null ? cursorOrder.getCreatedAt() : null,
                OrderStatus.PENDING,
                OrderStatus.CONFIRMED,
                OrderStatus.PACKING,
                OrderStatus.WAITING_GHTK_PICKUP,
                OrderStatus.SHIPPING,
                OrderStatus.COMPLETED,
                OrderStatus.CANCELLED,
                PageRequest.of(0, normalizedSize + 1)
        );

        boolean hasNext = orders.size() > normalizedSize;
        List<CustomerOrder> pageOrders = orders.stream()
                .limit(normalizedSize)
                .toList();
        List<OrderListItemDTO> content = toListItemDTOs(pageOrders);
        Long nextCursor = hasNext && !content.isEmpty()
                ? content.get(content.size() - 1).getId()
                : null;

        return ScrollResponse.of(content, normalizedSize, nextCursor, hasNext);
    }

    @Override
    public OrderDTO getById(Long id) {
        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFound("OrderNotFound", "Không tìm thấy đơn hàng"));
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        Map<Long, Product> productsById = loadProductsById(items);
        return OrderMapper.toDTO(order, toItemDTOs(items, productsById));
    }

    public OrderListItemDTO getDetail(Long shopId, Long id) {
        CustomerOrder order = orderRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new OrderNotFound("OrderNotFound", "Không tìm thấy đơn hàng"));
        return toListItemDTOs(List.of(order)).get(0);
    }

    public OrderListItemDTO getCustomerOrderDetail(Long userId, Long id) {
        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFound("OrderNotFound", "Không tìm thấy đơn hàng"));
        if (order.getUserId() == null || !order.getUserId().equals(userId)) {
            throw new OrderValidationException("OrderAccessDenied", "Bạn không có quyền xem đơn hàng này");
        }
        return toListItemDTOs(List.of(order)).get(0);
    }

    @Override
    @Transactional
    public OrderDTO create(OrderDTO dto) {
        validateOnlineRequestDoesNotUseCustomerAddress(dto, dto.getSource());
        CustomerOrder entity = OrderMapper.toEntity(dto);
        normalizeUserAndCustomer(entity);
        applyCustomerAddressSnapshotIfPresent(entity);
        validateOnlineShippingInfo(entity);
        List<OrderItem> items = buildItems(dto.getItems(), entity.getShopId(), null);
        applyTotals(entity, items, dto);

        CustomerOrder saved = orderRepository.save(entity);
        if (isBlank(saved.getOrderCode())) {
            saved.setOrderCode(formatOrderCode(saved.getId()));
            saved = orderRepository.save(saved);
        }

        for (OrderItem item : items) {
            item.setOrderId(saved.getId());
        }
        orderItemRepository.saveAll(items);

        OrderDTO result = getById(saved.getId());
        publishOrderCreatedNotification(saved);
        return result;
    }

    @Override
    @Transactional
    public OrderDTO update(Long id, OrderDTO dto) {
        CustomerOrder entity = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFound("OrderNotFound", "Không tìm thấy đơn hàng"));

        validateOnlineRequestDoesNotUseCustomerAddress(
                dto,
                dto.getSource() != null ? dto.getSource() : entity.getSource()
        );
        OrderMapper.updateEntity(entity, dto);
        if (dto.getUserId() != null || dto.getCustomerId() != null) {
            normalizeUserAndCustomer(entity);
        }
        if (dto.getCustomerAddressId() != null
                || dto.getUserAddressId() != null
                || dto.getCustomerId() != null
                || dto.getUserId() != null) {
            applyCustomerAddressSnapshotIfPresent(entity);
        }
        validateOnlineShippingInfo(entity);
        if (dto.getItems() != null) {
            orderItemRepository.deleteByOrderId(id);
            List<OrderItem> replacementItems = buildItems(dto.getItems(), entity.getShopId(), id);
            applyTotals(entity, replacementItems, dto);
            orderItemRepository.saveAll(replacementItems);
        } else {
            recalculateTotal(entity);
        }

        CustomerOrder saved = orderRepository.save(entity);
        return getById(saved.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFound("OrderNotFound", "Không tìm thấy đơn hàng");
        }
        orderRepository.deleteById(id);
    }

    private List<OrderDTO> toDTOs(List<CustomerOrder> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream().map(CustomerOrder::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrderId = orderItemRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        Map<Long, Product> productsById = loadProductsById(
                itemsByOrderId.values().stream().flatMap(List::stream).toList()
        );

        return orders.stream()
                .map(order -> OrderMapper.toDTO(
                        order,
                        toItemDTOs(itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList()), productsById)
                ))
                .toList();
    }

    private List<OrderListItemDTO> toListItemDTOs(List<CustomerOrder> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream().map(CustomerOrder::getId).toList();
        List<Long> userIds = orders.stream()
                .map(CustomerOrder::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, List<OrderItem>> itemsByOrderId = orderItemRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        Map<Long, Product> productsById = loadProductsById(
                itemsByOrderId.values().stream().flatMap(List::stream).toList()
        );

        return orders.stream()
                .map(order -> OrderMapper.toListItemDTO(
                        order,
                        usersById.get(order.getUserId()),
                        toItemDTOs(itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList()), productsById),
                        toStatusLabel(order.getStatus())
                ))
                .toList();
    }

    private List<OrderItem> buildItems(List<OrderItemDTO> itemDtos, Long shopId, Long orderId) {
        if (itemDtos == null || itemDtos.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = itemDtos.stream()
                .map(OrderItemDTO::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return itemDtos.stream()
                .map(dto -> buildItem(dto, shopId, orderId, productsById))
                .toList();
    }

    private OrderItem buildItem(
            OrderItemDTO dto,
            Long shopId,
            Long orderId,
            Map<Long, Product> productsById
    ) {
        if (dto.getProductId() == null) {
            throw new OrderValidationException("OrderProductRequired", "Sản phẩm trong đơn hàng không được để trống");
        }

        Product product = productsById.get(dto.getProductId());
        if (product == null || !Objects.equals(product.getShopId(), shopId)) {
            throw new OrderValidationException("OrderProductInvalid", "Sản phẩm trong đơn hàng không thuộc shop");
        }

        int qty = dto.getQty() != null ? dto.getQty() : 1;
        if (qty <= 0) {
            throw new OrderValidationException("OrderItemQtyInvalid", "Số lượng sản phẩm phải lớn hơn 0");
        }

        long unitPrice = dto.getUnitPrice() != null ? dto.getUnitPrice() : product.getPrice();
        if (unitPrice < 0) {
            throw new OrderValidationException("OrderItemPriceInvalid", "Đơn giá sản phẩm phải lớn hơn hoặc bằng 0");
        }

        OrderItem item = new OrderItem();
        item.setShopId(shopId);
        item.setOrderId(orderId);
        item.setProductId(dto.getProductId());
        item.setQty(qty);
        item.setUnitPrice(unitPrice);
        item.setAmount(unitPrice * qty);
        return item;
    }

    private void applyTotals(CustomerOrder entity, List<OrderItem> items, OrderDTO dto) {
        long subtotal = !items.isEmpty()
                ? items.stream().mapToLong(OrderItem::getAmount).sum()
                : valueOrZero(dto.getSubtotalAmount());
        entity.setSubtotalAmount(subtotal);
        entity.setShippingFee(valueOrZero(dto.getShippingFee()));
        entity.setDiscountAmount(valueOrZero(dto.getDiscountAmount()));
        recalculateTotal(entity);
    }

    private void recalculateTotal(CustomerOrder entity) {
        long total = valueOrZero(entity.getSubtotalAmount())
                + valueOrZero(entity.getShippingFee())
                - valueOrZero(entity.getDiscountAmount());
        if (total < 0) {
            throw new OrderValidationException("OrderTotalInvalid", "Tổng tiền đơn hàng phải lớn hơn hoặc bằng 0");
        }
        entity.setTotalAmount(total);
    }

    private Map<Long, Product> loadProductsById(List<OrderItem> items) {
        List<Long> productIds = items.stream()
                .map(OrderItem::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private List<OrderItemDTO> toItemDTOs(List<OrderItem> items, Map<Long, Product> productsById) {
        return items.stream()
                .map(item -> {
                    Product product = productsById.get(item.getProductId());
                    return new OrderItemDTO(
                            item.getId(),
                            item.getShopId(),
                            item.getOrderId(),
                            item.getProductId(),
                            product != null ? product.getName() : "Product #" + item.getProductId(),
                            item.getQty(),
                            item.getUnitPrice(),
                            item.getAmount(),
                            item.getCreatedAt()
                    );
                })
                .toList();
    }

    private String formatOrderCode(Long id) {
        return id != null ? "ORD-" + String.format("%03d", id) : null;
    }

    private void normalizeUserAndCustomer(CustomerOrder order) {
        Customer customer = null;
        if (order.getCustomerId() != null) {
            customer = customerRepository.findByShopIdAndId(order.getShopId(), order.getCustomerId())
                    .orElseThrow(() -> new OrderValidationException(
                            "OrderCustomerNotFound",
                            "Không tìm thấy khách hàng của shop"
                    ));
            if (order.getUserId() == null) {
                order.setUserId(customer.getUserId());
            } else if (customer.getUserId() != null && !Objects.equals(order.getUserId(), customer.getUserId())) {
                throw new OrderValidationException(
                        "OrderUserCustomerMismatch",
                        "userId không khớp với customerId của shop"
                );
            }
        }

        if (order.getUserId() != null) {
            assertActiveUser(order.getUserId());
            if (order.getCustomerId() == null) {
                customerRepository.findFirstByShopIdAndUserIdOrderByIdDesc(order.getShopId(), order.getUserId())
                        .ifPresent(linkedCustomer -> order.setCustomerId(linkedCustomer.getId()));
            }
            return;
        }

        if (order.getSource() == OrderSource.ONLINE) {
            throw new OrderValidationException(
                    "OrderUserRequired",
                    "Đơn online cần có userId"
            );
        }
    }

    private void validateOnlineRequestDoesNotUseCustomerAddress(OrderDTO dto, OrderSource targetSource) {
        OrderSource normalizedSource = targetSource != null ? targetSource : OrderSource.ONLINE;
        if (normalizedSource != OrderSource.ONLINE) {
            return;
        }
        if (dto.getCustomerId() != null || dto.getCustomerAddressId() != null) {
            throw new OrderValidationException(
                    "OrderOnlineCustomerFieldNotAllowed",
                    "Đơn online dùng userId/userAddressId, không dùng customerId/customerAddressId"
            );
        }
    }

    private void assertActiveUser(Long userId) {
        if (!userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)) {
            throw new OrderValidationException(
                    "OrderUserNotFound",
                    "Không tìm thấy người dùng đang hoạt động"
            );
        }
    }

    private void publishOrderCreatedNotification(CustomerOrder order) {
        if (order.getSource() != OrderSource.ONLINE) {
            return;
        }

        Customer customer = resolveNotificationCustomer(order.getShopId(), order.getCustomerId());
        User user = resolveNotificationUser(order.getUserId());
        String displayName = user != null ? user.getFullName() : customer != null ? customer.getFullName() : null;

        notificationService.publishToShop(
                order.getShopId(),
                NotificationType.ORDER_CREATED,
                NotificationTargetType.ORDER,
                order.getId(),
                order.getUserId(),
                "Đơn hàng online mới",
                displayName != null && !displayName.isBlank()
                        ? "Có đơn hàng online mới từ " + displayName
                        : "Có đơn hàng online mới",
                buildOrderNotificationMetadata(order, user, customer)
        );
    }

    private Map<String, Object> buildOrderNotificationMetadata(CustomerOrder order, User user, Customer customer) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("orderId", order.getId());
        metadata.put("orderCode", order.getOrderCode());
        metadata.put("userId", order.getUserId());
        metadata.put("userFullName", user != null ? user.getFullName() : null);
        metadata.put("customerId", order.getCustomerId());
        metadata.put("customerName", customer != null ? customer.getFullName() : null);
        metadata.put("source", order.getSource());
        metadata.put("totalAmount", order.getTotalAmount());
        metadata.put("senderUserId", order.getUserId());
        metadata.put("senderAvatarUrl", user != null ? user.getAvatarUrlPreview() : null);
        return metadata;
    }

    private Customer resolveNotificationCustomer(Long shopId, Long customerId) {
        if (shopId == null || customerId == null) {
            return null;
        }
        return customerRepository.findByShopIdAndId(shopId, customerId).orElse(null);
    }

    private User resolveNotificationUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    private void applyCustomerAddressSnapshotIfPresent(CustomerOrder order) {
        UserAddress userAddress = resolveUserAddress(order, false);
        if (userAddress != null) {
            copyUserAddressToOrder(order, userAddress);
            return;
        }

        CustomerAddress customerAddress = resolveCustomerAddress(order, false);
        if (customerAddress == null) {
            return;
        }
        copyCustomerAddressToOrder(order, customerAddress);
    }

    private UserAddress resolveUserAddress(CustomerOrder order, boolean required) {
        if (order.getUserAddressId() != null) {
            if (order.getUserId() == null) {
                throw new OrderValidationException(
                        "OrderUserRequired",
                        "Đơn online cần có userId để dùng userAddressId"
                );
            }
            return userAddressRepository.findByIdAndUserId(order.getUserAddressId(), order.getUserId())
                    .orElseThrow(() -> new OrderValidationException(
                            "OrderUserAddressNotFound",
                            "Không tìm thấy địa chỉ của người dùng"
                    ));
        }

        if (order.getSource() == OrderSource.ONLINE && order.getUserId() != null) {
            UserAddress defaultAddress = userAddressRepository
                    .findFirstByUserIdOrderByDefaultAddressDescIdDesc(order.getUserId())
                    .orElse(null);
            if (defaultAddress != null) {
                order.setUserAddressId(defaultAddress.getId());
                return defaultAddress;
            }
        }

        if (required) {
            throw new OrderValidationException(
                    "OrderUserAddressRequired",
                    "Đơn online cần có địa chỉ giao hàng của người dùng"
            );
        }
        return null;
    }

    private CustomerAddress resolveCustomerAddress(CustomerOrder order, boolean required) {
        if (order.getCustomerAddressId() != null) {
            CustomerAddress address = customerAddressRepository.findById(order.getCustomerAddressId())
                    .orElseThrow(() -> new OrderValidationException(
                            "OrderCustomerAddressNotFound",
                            "Không tìm thấy địa chỉ nhận hàng của khách hàng"
                    ));
            validateCustomerAddressMatchesOrderCustomer(order, address);
            return address;
        }

        if (order.getCustomerId() == null) {
            if (required) {
                throw new OrderValidationException(
                        "OrderCustomerRequired",
                        "Đơn hàng cần gắn khách hàng để lấy địa chỉ nhận hàng"
                );
            }
            return null;
        }

        CustomerAddress address = customerAddressRepository
                .findFirstByCustomerIdOrderByDefaultAddressDescIdDesc(order.getCustomerId())
                .orElse(null);
        if (address == null && required) {
            throw new OrderValidationException(
                    "OrderCustomerAddressRequired",
                    "Khách hàng của đơn hàng chưa có địa chỉ nhận hàng"
            );
        }
        return address;
    }

    private void validateCustomerAddressMatchesOrderCustomer(CustomerOrder order, CustomerAddress address) {
        if (order.getCustomerId() == null) {
            return;
        }

        Customer customer = customerRepository.findByShopIdAndId(order.getShopId(), order.getCustomerId())
                .orElseThrow(() -> new OrderValidationException(
                        "OrderCustomerNotFound",
                        "Không tìm thấy khách hàng của đơn hàng"
                ));
        if (!Objects.equals(customer.getId(), address.getCustomerId())) {
            throw new OrderValidationException(
                    "OrderCustomerAddressMismatch",
                    "Địa chỉ nhận hàng không thuộc khách hàng trong đơn"
            );
        }
    }

    private void copyCustomerAddressToOrder(CustomerOrder order, CustomerAddress address) {
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

    private void copyUserAddressToOrder(CustomerOrder order, UserAddress address) {
        order.setUserAddressId(address.getId());
        order.setReceiverName(address.getName());
        order.setReceiverPhone(address.getTel());
        order.setShippingAddress(address.getAddress());
        order.setShippingProvince(address.getProvince());
        order.setShippingDistrict(address.getDistrict());
        order.setShippingWard(address.getWard());
        order.setShippingStreet(null);
        order.setShippingHamlet(address.getHamlet());
    }

    private void validateOnlineShippingInfo(CustomerOrder order) {
        if (order.getSource() != OrderSource.ONLINE) {
            return;
        }
        if (isBlank(order.getReceiverName())
                || isBlank(order.getReceiverPhone())
                || isBlank(order.getShippingAddress())
                || isBlank(order.getShippingProvince())
                || isBlank(order.getShippingDistrict())
                || isBlank(order.getShippingWard())) {
            throw new OrderValidationException(
                    "OrderShippingInfoRequired",
                    "Đơn online cần có userAddressId hoặc đủ thông tin giao hàng"
            );
        }
    }

    private String toStatusLabel(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PENDING -> "Chờ xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case PACKING -> "Đang đóng gói";
            case WAITING_GHTK_PICKUP -> "Chờ GHTK đến lấy hàng";
            case SHIPPING -> "Đang giao";
            case COMPLETED -> "Hoàn thành";
            case CANCELLED -> "Đã hủy";
        };
    }

    private CustomerOrder resolveCursorOrder(Long shopId, Long cursor) {
        if (cursor == null) {
            return null;
        }
        if (shopId == null) {
            return orderRepository.findById(cursor).orElse(null);
        }
        return orderRepository.findByIdAndShopId(cursor, shopId).orElse(null);
    }

    private int toSortPriority(OrderStatus status) {
        if (status == null) {
            return 6;
        }
        return switch (status) {
            case PENDING -> 0;
            case CONFIRMED -> 1;
            case PACKING -> 2;
            case WAITING_GHTK_PICKUP -> 3;
            case SHIPPING -> 4;
            case COMPLETED -> 5;
            case CANCELLED -> 6;
        };
    }

    private long valueOrZero(Long value) {
        return value != null ? value : 0L;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private OffsetDateTime toStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay(BUSINESS_ZONE).toOffsetDateTime() : null;
    }

    private OffsetDateTime toStartOfNextDay(LocalDate date) {
        return date != null ? date.plusDays(1).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime() : null;
    }
}
