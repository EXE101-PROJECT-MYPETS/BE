package com.exe101.ghtk.webhook;

import com.exe101.commission.service.CommissionService;
import com.exe101.notification.dto.NotificationTargetType;
import com.exe101.notification.dto.NotificationType;
import com.exe101.notification.service.NotificationService;
import com.exe101.order.entity.CustomerOrder;
import com.exe101.order.entity.OrderStatus;
import com.exe101.order.repository.IOrderRepository;
import com.exe101.shipping.entity.ShipmentStatus;
import com.exe101.shipping.entity.ShippingWebhookLog;
import com.exe101.shipping.entity.ShippingWebhookProcessingStatus;
import com.exe101.shipping.entity.ShopOrderShipment;
import com.exe101.shipping.repository.IShippingWebhookLogRepository;
import com.exe101.shipping.repository.IShopOrderShipmentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GhtkWebhookService {

    private final IShopOrderShipmentRepository shipmentRepository;
    private final IShippingWebhookLogRepository webhookLogRepository;
    private final IOrderRepository orderRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final CommissionService commissionService;

    @Value("${ghtk.webhook-secret:}")
    private String webhookSecret;

    public boolean isValidHash(String hash) {
        if (isBlank(webhookSecret) || isBlank(hash)) {
            return false;
        }
        return MessageDigest.isEqual(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                hash.trim().getBytes(StandardCharsets.UTF_8)
        );
    }

    @Transactional
    public void handle(String hash, MultiValueMap<String, String> formData) {
        MultiValueMap<String, String> payloadForm = copyWithoutHash(formData);
        GhtkWebhookPayload payload = GhtkWebhookPayload.fromForm(payloadForm);
        JsonNode rawPayloadJson = toRawPayloadJson(payloadForm);
        Integer statusId = parseInteger(payload.statusId());
        OffsetDateTime actionTime = parseActionTime(payload.actionTime());

        Optional<ShopOrderShipment> shipmentOptional = findShipment(payload);
        if (shipmentOptional.isEmpty()) {
            ShippingWebhookLog log = createLog(hash, payload, statusId, actionTime, rawPayloadJson);
            log.setProcessingStatus(ShippingWebhookProcessingStatus.UNKNOWN_SHIPMENT);
            webhookLogRepository.save(log);
            return;
        }

        ShopOrderShipment shipment = shipmentOptional.get();
        ShippingWebhookLog log = createLog(hash, payload, statusId, actionTime, rawPayloadJson);
        log.setShipmentId(shipment.getId());
        log.setShopId(shipment.getShopId());
        log.setOrderId(shipment.getOrderId());
        log.setProcessingStatus(ShippingWebhookProcessingStatus.RECEIVED);
        log = webhookLogRepository.save(log);

        try {
            processWebhook(log, shipment, payload, statusId, actionTime);
        } catch (RuntimeException ex) {
            log.setProcessingStatus(ShippingWebhookProcessingStatus.FAILED);
            log.setErrorMessage(ex.getMessage());
            webhookLogRepository.save(log);
            throw ex;
        }
    }

    private void processWebhook(
            ShippingWebhookLog log,
            ShopOrderShipment shipment,
            GhtkWebhookPayload payload,
            Integer statusId,
            OffsetDateTime actionTime
    ) {
        if (statusId == null) {
            markFailed(log, "GHTK webhook thieu status_id hop le");
            return;
        }
        if (actionTime == null) {
            markFailed(log, "GHTK webhook thieu action_time hop le");
            return;
        }

        OffsetDateTime lastActionTime = shipment.getLastActionTime();
        if (lastActionTime != null && !actionTime.isAfter(lastActionTime)) {
            log.setProcessingStatus(ShippingWebhookProcessingStatus.STALE_OR_DUPLICATE);
            webhookLogRepository.save(log);
            return;
        }

        if (GhtkShipmentStatusMapper.isShipperOnlyStatus(statusId)) {
            log.setProcessingStatus(ShippingWebhookProcessingStatus.SHIPPER_EVENT);
            webhookLogRepository.save(log);
            return;
        }

        ShipmentStatus shipmentStatus = GhtkShipmentStatusMapper.toShipmentStatus(statusId);
        if (shipmentStatus == null) {
            markFailed(log, "GHTK status_id khong duoc ho tro: " + statusId);
            return;
        }

        shipment.setStatus(shipmentStatus);
        shipment.setGhtkStatusId(statusId);
        shipment.setLastActionTime(actionTime);
        shipment.setActualShippingFee(parseLong(payload.fee()));
        shipment.setWeight(parseBigDecimal(payload.weight()));
        shipment.setPickMoney(parseLong(payload.pickMoney()));
        shipment.setReturnPartPackage(parseInteger(payload.returnPartPackage()));
        shipment.setReasonCode(trim(payload.reasonCode()));
        shipment.setReason(trim(payload.reason()));
        shipmentRepository.save(shipment);

        updateOrderForImportantShipmentStatus(shipment, shipmentStatus);

        log.setProcessingStatus(ShippingWebhookProcessingStatus.APPLIED);
        webhookLogRepository.save(log);
    }

    private void updateOrderForImportantShipmentStatus(ShopOrderShipment shipment, ShipmentStatus shipmentStatus) {
        OrderStatus nextStatus = toImportantOrderStatus(shipmentStatus);
        if (nextStatus == null) {
            return;
        }

        CustomerOrder order = orderRepository.findByIdAndShopIdForUpdate(shipment.getOrderId(), shipment.getShopId())
                .orElse(null);
        if (order == null || !canMoveOrderStatus(order.getStatus(), nextStatus)) {
            return;
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(nextStatus);
        CustomerOrder savedOrder = orderRepository.save(order);
        if (nextStatus == OrderStatus.COMPLETED) {
            commissionService.createCommissionIfAbsent(savedOrder);
        }
        publishOrderStatusNotification(savedOrder, previousStatus, nextStatus);
    }

    private OrderStatus toImportantOrderStatus(ShipmentStatus shipmentStatus) {
        return switch (shipmentStatus) {
            case PICKED_UP -> OrderStatus.GHTK_PICKED_UP;
            case DELIVERING -> OrderStatus.SHIPPING;
            case DELIVERED, RECONCILED -> OrderStatus.COMPLETED;
            case CANCELED -> OrderStatus.CANCELLED;
            default -> null;
        };
    }

    private boolean canMoveOrderStatus(OrderStatus currentStatus, OrderStatus nextStatus) {
        if (currentStatus == null || currentStatus == nextStatus) {
            return false;
        }
        if (currentStatus == OrderStatus.COMPLETED || currentStatus == OrderStatus.CANCELLED) {
            return false;
        }
        if (nextStatus == OrderStatus.GHTK_PICKED_UP) {
            return currentStatus == OrderStatus.CONFIRMED
                    || currentStatus == OrderStatus.PACKING
                    || currentStatus == OrderStatus.WAITING_GHTK_PICKUP;
        }
        if (nextStatus == OrderStatus.SHIPPING) {
            return currentStatus == OrderStatus.CONFIRMED
                    || currentStatus == OrderStatus.PACKING
                    || currentStatus == OrderStatus.WAITING_GHTK_PICKUP
                    || currentStatus == OrderStatus.GHTK_PICKED_UP;
        }
        if (nextStatus == OrderStatus.COMPLETED) {
            return true;
        }
        if (nextStatus == OrderStatus.CANCELLED) {
            return currentStatus != OrderStatus.COMPLETED;
        }
        return false;
    }

    private void publishOrderStatusNotification(
            CustomerOrder order,
            OrderStatus previousStatus,
            OrderStatus nextStatus
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("orderId", order.getId());
        metadata.put("orderCode", order.getOrderCode());
        metadata.put("previousStatus", previousStatus);
        metadata.put("status", nextStatus);

        notificationService.publishToShop(
                order.getShopId(),
                NotificationType.ORDER_STATUS_UPDATED,
                NotificationTargetType.ORDER,
                order.getId(),
                null,
                "Cap nhat van chuyen GHTK",
                "Don hang " + resolveOrderCode(order) + " da chuyen sang " + toOrderStatusLabel(nextStatus),
                metadata
        );

        if (order.getUserId() != null) {
            notificationService.publishToUser(
                    order.getUserId(),
                    order.getShopId(),
                    NotificationType.ORDER_STATUS_UPDATED,
                    NotificationTargetType.ORDER,
                    order.getId(),
                    null,
                    "Cap nhat don hang",
                    "Don hang " + resolveOrderCode(order) + " da chuyen sang " + toOrderStatusLabel(nextStatus),
                    metadata
            );
        }
    }

    private Optional<ShopOrderShipment> findShipment(GhtkWebhookPayload payload) {
        if (!isBlank(payload.partnerId())) {
            Optional<ShopOrderShipment> shipment = shipmentRepository.findByPartnerIdForUpdate(payload.partnerId());
            if (shipment.isPresent()) {
                return shipment;
            }
        }
        if (!isBlank(payload.labelId())) {
            return shipmentRepository.findByLabelIdForUpdate(payload.labelId());
        }
        return Optional.empty();
    }

    private ShippingWebhookLog createLog(
            String hash,
            GhtkWebhookPayload payload,
            Integer statusId,
            OffsetDateTime actionTime,
            JsonNode rawPayloadJson
    ) {
        ShippingWebhookLog log = new ShippingWebhookLog();
        log.setCarrier(ShopOrderShipment.CARRIER_GHTK);
        log.setPartnerId(trim(payload.partnerId()));
        log.setLabelId(trim(payload.labelId()));
        log.setStatusId(statusId);
        log.setActionTime(actionTime);
        log.setHash(trim(hash));
        log.setRawPayloadJson(rawPayloadJson);
        return log;
    }

    private void markFailed(ShippingWebhookLog log, String message) {
        log.setProcessingStatus(ShippingWebhookProcessingStatus.FAILED);
        log.setErrorMessage(message);
        webhookLogRepository.save(log);
    }

    private JsonNode toRawPayloadJson(MultiValueMap<String, String> formData) {
        Map<String, String> payload = new LinkedHashMap<>();
        formData.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                payload.put(key, values.get(0));
            }
        });
        return objectMapper.valueToTree(payload);
    }

    private MultiValueMap<String, String> copyWithoutHash(MultiValueMap<String, String> formData) {
        MultiValueMap<String, String> copy = new org.springframework.util.LinkedMultiValueMap<>(formData);
        copy.remove("hash");
        return copy;
    }

    private OffsetDateTime parseActionTime(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
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

    private BigDecimal parseBigDecimal(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveOrderCode(CustomerOrder order) {
        if (!isBlank(order.getOrderCode())) {
            return order.getOrderCode();
        }
        return "#" + order.getId();
    }

    private String toOrderStatusLabel(OrderStatus status) {
        return switch (status) {
            case GHTK_PICKED_UP -> "ghtk da lay hang";
            case SHIPPING -> "dang giao";
            case COMPLETED -> "hoan thanh";
            case CANCELLED -> "da huy";
            default -> status.name();
        };
    }

    private String trim(String value) {
        return value != null ? value.trim() : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
