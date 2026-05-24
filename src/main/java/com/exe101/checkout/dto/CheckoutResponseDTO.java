package com.exe101.checkout.dto;

import java.util.ArrayList;
import java.util.List;

public class CheckoutResponseDTO {

    private Long orderId;
    private String orderCode;
    private List<Long> bookingIds = new ArrayList<>();
    private Long productSubtotalAmount = 0L;
    private Long serviceSubtotalAmount = 0L;
    private Long subtotalAmount = 0L;
    private Long shippingFee = 0L;
    private Long pickupFee = 0L;
    private Long discountAmount = 0L;
    private Long totalAmount = 0L;

    public CheckoutResponseDTO() {
    }

    public CheckoutResponseDTO(
            Long orderId,
            String orderCode,
            List<Long> bookingIds,
            Long productSubtotalAmount,
            Long serviceSubtotalAmount,
            Long subtotalAmount,
            Long shippingFee,
                Long pickupFee,
            Long discountAmount,
            Long totalAmount
    ) {
        this.orderId = orderId;
        this.orderCode = orderCode;
        this.bookingIds = bookingIds != null ? bookingIds : new ArrayList<>();
        this.productSubtotalAmount = productSubtotalAmount;
        this.serviceSubtotalAmount = serviceSubtotalAmount;
        this.subtotalAmount = subtotalAmount;
        this.shippingFee = shippingFee;
        this.pickupFee = pickupFee;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public List<Long> getBookingIds() {
        return bookingIds;
    }

    public void setBookingIds(List<Long> bookingIds) {
        this.bookingIds = bookingIds;
    }

    public Long getProductSubtotalAmount() {
        return productSubtotalAmount;
    }

    public void setProductSubtotalAmount(Long productSubtotalAmount) {
        this.productSubtotalAmount = productSubtotalAmount;
    }

    public Long getServiceSubtotalAmount() {
        return serviceSubtotalAmount;
    }

    public void setServiceSubtotalAmount(Long serviceSubtotalAmount) {
        this.serviceSubtotalAmount = serviceSubtotalAmount;
    }

    public Long getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(Long subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public Long getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(Long shippingFee) {
        this.shippingFee = shippingFee;
    }

    public Long getPickupFee() {
        return pickupFee;
    }

    public void setPickupFee(Long pickupFee) {
        this.pickupFee = pickupFee;
    }

    public Long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Long discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }
}