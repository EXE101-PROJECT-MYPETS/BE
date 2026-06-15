package com.exe101.manualPayment.service;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.booking.entity.*;
import com.exe101.booking.exception.BookingNotFound;
import com.exe101.booking.repository.IBookingItemRepository;
import com.exe101.booking.repository.IBookingRepository;
import com.exe101.booking.repository.IBookingStatusEventRepository;
import com.exe101.commission.service.CommissionService;
import com.exe101.inventory.entity.Inventory;
import com.exe101.inventory.repository.IInventoryRepository;
import com.exe101.invoice.entity.Invoice;
import com.exe101.invoice.entity.InvoiceStatus;
import com.exe101.invoice.exception.InvoiceNotFound;
import com.exe101.invoice.repository.IInvoiceRepository;
import com.exe101.manualPayment.dto.ManualPaymentConfirmRequest;
import com.exe101.manualPayment.dto.ManualPaymentConfirmResponse;
import com.exe101.manualPayment.exception.ManualPaymentAccessDenied;
import com.exe101.manualPayment.exception.ManualPaymentValidationException;
import com.exe101.order.entity.CustomerOrder;
import com.exe101.order.entity.OrderItem;
import com.exe101.order.entity.OrderStatus;
import com.exe101.order.exception.OrderNotFound;
import com.exe101.order.repository.IOrderItemRepository;
import com.exe101.order.repository.IOrderRepository;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import com.exe101.order.service.OrderService;
import com.exe101.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManualPaymentService {

    private final IInvoiceRepository invoiceRepository;
    private final IBookingRepository bookingRepository;
    private final IBookingItemRepository bookingItemRepository;
    private final IBookingStatusEventRepository bookingStatusEventRepository;
    private final IOrderRepository orderRepository;
    private final IOrderItemRepository orderItemRepository;
    private final IInventoryRepository inventoryRepository;
    private final IShopMemberRepository shopMemberRepository;
    private final OrderService orderService;
    private final BookingService bookingService;
    private final CommissionService commissionService;

    @Transactional
    public ManualPaymentConfirmResponse confirmPayment(Long shopId, ManualPaymentConfirmRequest request) {
        assertCanConfirmPayment(shopId);
        assertSinglePaymentSource(request);

        Invoice invoice = invoiceRepository.findByIdAndShopId(request.getInvoiceId(), shopId)
                .orElseThrow(() -> new InvoiceNotFound(
                        "InvoiceNotFound",
                        "Không tìm thấy hóa đơn"
                ));

        if (request.getOrderId() != null) {
            return confirmOrderPayment(shopId, invoice, request);
        }

        return confirmBookingPayment(shopId, invoice, request);
    }

    private ManualPaymentConfirmResponse confirmOrderPayment(
            Long shopId,
            Invoice invoice,
            ManualPaymentConfirmRequest request
    ) {
        CustomerOrder order = orderRepository.findByIdAndShopId(request.getOrderId(), shopId)
                .orElseThrow(() -> new OrderNotFound(
                        "OrderNotFound",
                        "Không tìm thấy đơn hàng"
                ));

        validatePaymentRequest(invoice, order, request);
        deductInventoryForOrder(shopId, order.getId());

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentMethod(request.getPaymentMethod());
        order.setStatus(OrderStatus.COMPLETED);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        CustomerOrder savedOrder = orderRepository.save(order);
commissionService.createCommissionIfAbsent(savedOrder);

try {
    orderService.publishOrderStatusUpdatedNotification(savedOrder);
} catch (Exception e) {
    log.error("Failed to publish order status update notification", e);
}

        return new ManualPaymentConfirmResponse(
                savedInvoice.getId(),
                savedOrder.getId(),
                null,
                request.getPaidAmount(),
                savedInvoice.getPaymentMethod(),
                savedInvoice.getStatus(),
                savedOrder.getStatus(),
                null,
                OffsetDateTime.now()
        );
    }

    private ManualPaymentConfirmResponse confirmBookingPayment(
            Long shopId,
            Invoice invoice,
            ManualPaymentConfirmRequest request
    ) {
        Booking booking = bookingRepository.findByIdAndShopId(request.getBookingId(), shopId)
                .orElseThrow(() -> new BookingNotFound(
                        "BookingNotFound",
                        "Không tìm thấy lịch hẹn"
                ));

        validateBookingPaymentRequest(invoice, booking, request);
        deductInventoryForBooking(shopId, booking.getId());

        BookingStatus previousStatus = booking.getStatus();
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentMethod(request.getPaymentMethod());
        booking.setStatus(BookingStatus.COMPLETED);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        Booking savedBooking = bookingRepository.save(booking);
        commissionService.createCommissionIfAbsent(savedBooking);
        createBookingStatusEvent(
                savedBooking.getShopId(),
                savedBooking.getId(),
                previousStatus,
                savedBooking.getStatus()
        );
        try {
            bookingService.publishBookingStatusUpdatedNotification(savedBooking);
        } catch (Exception e) {
            // ignore/log
        }

        return new ManualPaymentConfirmResponse(
                savedInvoice.getId(),
                null,
                savedBooking.getId(),
                request.getPaidAmount(),
                savedInvoice.getPaymentMethod(),
                savedInvoice.getStatus(),
                null,
                savedBooking.getStatus(),
                OffsetDateTime.now()
        );
    }

    private void createBookingStatusEvent(
            Long shopId,
            Long bookingId,
            BookingStatus fromStatus,
            BookingStatus toStatus
    ) {
        if (Objects.equals(fromStatus, toStatus)) {
            return;
        }

        BookingStatusEvent event = new BookingStatusEvent();
        event.setShopId(shopId);
        event.setBookingId(bookingId);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setActorUserId(getCurrentUserId());
        bookingStatusEventRepository.save(event);
    }

    private void assertSinglePaymentSource(ManualPaymentConfirmRequest request) {
        boolean hasOrderId = request.getOrderId() != null;
        boolean hasBookingId = request.getBookingId() != null;

        if (hasOrderId == hasBookingId) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentSingleSourceRequired",
                    "Chỉ được gửi một trong hai trường orderId hoặc bookingId"
            );
        }
    }

    private void validateBookingPaymentRequest(
            Invoice invoice,
            Booking booking,
            ManualPaymentConfirmRequest request
    ) {
        if (invoice.getBookingId() == null) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentInvoiceBookingRequired",
                    "Hóa đơn này không gắn với lịch hẹn"
            );
        }
        if (!Objects.equals(invoice.getBookingId(), request.getBookingId())) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentBookingMismatch",
                    "Hóa đơn không khớp với lịch hẹn cần xác nhận"
            );
        }
        if (!Objects.equals(booking.getId(), invoice.getBookingId())) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentBookingMismatch",
                    "Lịch hẹn không khớp với hóa đơn cần xác nhận"
            );
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentInvoiceAlreadyPaid",
                    "Hóa đơn đã được xác nhận thanh toán"
            );
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED || invoice.getStatus() == InvoiceStatus.VOID) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentInvoiceClosed",
                    "Không thể xác nhận thanh toán cho hóa đơn đã hủy hoặc đã vô hiệu"
            );
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentBookingClosed",
                    "Không thể xác nhận thanh toán cho lịch hẹn đã hủy hoặc đã từ chối"
            );
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentBookingAlreadyCompleted",
                    "Lịch hẹn đã hoàn thành, không thể xác nhận thanh toán lại"
            );
        }
        long bookingTotal = bookingItemRepository.findByBookingId(booking.getId()).stream()
                .mapToLong(item -> item.getAmount() != null ? item.getAmount() : 0L)
                .sum();
        if (!Objects.equals(invoice.getTotalAmount(), bookingTotal)) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentBookingInvoiceAmountMismatch",
                    "Tổng tiền hóa đơn không khớp với tổng tiền checkout của lịch hẹn"
            );
        }
        if (!Objects.equals(invoice.getTotalAmount(), request.getPaidAmount())) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentAmountMismatch",
                    "Số tiền thanh toán không khớp với tổng tiền hóa đơn"
            );
        }
    }

    private void validatePaymentRequest(
            Invoice invoice,
            CustomerOrder order,
            ManualPaymentConfirmRequest request
    ) {
        if (invoice.getOrderId() == null) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentInvoiceOrderRequired",
                    "Hóa đơn này không gắn với đơn hàng"
            );
        }
        if (!Objects.equals(invoice.getOrderId(), request.getOrderId())) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentOrderMismatch",
                    "Hóa đơn không khớp với đơn hàng cần xác nhận"
            );
        }
        if (!Objects.equals(order.getId(), invoice.getOrderId())) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentOrderMismatch",
                    "Đơn hàng không khớp với hóa đơn cần xác nhận"
            );
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentInvoiceAlreadyPaid",
                    "Hóa đơn đã được xác nhận thanh toán"
            );
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED || invoice.getStatus() == InvoiceStatus.VOID) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentInvoiceClosed",
                    "Không thể xác nhận thanh toán cho hóa đơn đã hủy hoặc đã vô hiệu"
            );
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentOrderCancelled",
                    "Không thể xác nhận thanh toán cho đơn hàng đã hủy"
            );
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentOrderAlreadyCompleted",
                    "Đơn hàng đã hoàn thành, không thể xác nhận thanh toán lại"
            );
        }
        if (!Objects.equals(invoice.getTotalAmount(), order.getTotalAmount())) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentOrderInvoiceAmountMismatch",
                    "Tổng tiền hóa đơn không khớp với tổng tiền đơn hàng"
            );
        }
        if (!Objects.equals(invoice.getTotalAmount(), request.getPaidAmount())) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentAmountMismatch",
                    "Số tiền thanh toán không khớp với tổng tiền hóa đơn"
            );
        }
    }

    private void deductInventoryForOrder(Long shopId, Long orderId) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        if (orderItems.isEmpty()) {
            throw new ManualPaymentValidationException(
                    "ManualPaymentOrderItemsRequired",
                    "Đơn hàng không có sản phẩm để trừ tồn kho"
            );
        }

        Map<Long, Long> qtyByProductId = orderItems.stream()
                .collect(Collectors.groupingBy(
                        OrderItem::getProductId,
                        Collectors.summingLong(item -> item.getQty() != null ? item.getQty() : 0L)
                ));
        deductInventoryByProductQty(shopId, qtyByProductId);
    }

    private void deductInventoryForBooking(Long shopId, Long bookingId) {
        Map<Long, Long> qtyByProductId = bookingItemRepository.findByBookingId(bookingId).stream()
                .filter(item -> item.getItemType() == BookingItemType.PRODUCT)
                .filter(item -> item.getRefId() != null)
                .collect(Collectors.groupingBy(
                        BookingItem::getRefId,
                        Collectors.summingLong(item -> item.getQty() != null ? item.getQty() : 0L)
                ));
        deductInventoryByProductQty(shopId, qtyByProductId);
    }

    private void deductInventoryByProductQty(Long shopId, Map<Long, Long> qtyByProductId) {
        if (qtyByProductId == null || qtyByProductId.isEmpty()) {
            return;
        }

        List<Long> productIds = qtyByProductId.keySet().stream().toList();
        Map<Long, Inventory> inventoryByProductId = inventoryRepository
                .findByShopIdAndProductIdInForUpdate(shopId, productIds)
                .stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        for (Map.Entry<Long, Long> entry : qtyByProductId.entrySet()) {
            Long productId = entry.getKey();
            Long requestedQty = entry.getValue();
            Inventory inventory = inventoryByProductId.get(productId);
            if (inventory == null) {
                throw new ManualPaymentValidationException(
                        "ManualPaymentInventoryNotFound",
                        "Không tìm thấy tồn kho cho sản phẩm #" + productId
                );
            }

            long onHand = inventory.getOnHand() != null ? inventory.getOnHand() : 0L;
            long reserved = inventory.getReserved() != null ? inventory.getReserved() : 0L;
            long available = onHand - reserved;
            if (available < requestedQty) {
                throw new ManualPaymentValidationException(
                        "ManualPaymentInsufficientInventory",
                        "Tồn kho không đủ cho sản phẩm #" + productId
                );
            }

            inventory.setOnHand(onHand - requestedQty);
        }

        inventoryRepository.saveAll(inventoryByProductId.values());
    }

    private void assertCanConfirmPayment(Long shopId) {
        Long userId = getCurrentUserId();
        boolean allowed = shopMemberRepository.existsByShopIdAndUserIdAndStatus(
                shopId,
                userId,
                MemberStatus.ACTIVE
        );

        if (!allowed) {
            throw new ManualPaymentAccessDenied(
                    "ManualPaymentAccessDenied",
                    "Bạn không có quyền xác nhận thanh toán cho shop này"
            );
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new ManualPaymentAccessDenied(
                    "ManualPaymentAccessDenied",
                    "Bạn cần đăng nhập để xác nhận thanh toán"
            );
        }
        return userPrincipal.getUser().getId();
    }
}
