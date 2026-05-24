package com.exe101.checkout.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CheckoutRequestDTO {

    @NotNull(message = "Shop không được để trống")
    private Long shopId;

    @NotNull(message = "Người dùng không được để trống")
    private Long userId;

    private Long customerId;

    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;
    private Long shippingFee;
    private Long pickupFee = 0L;
    private Long discountAmount = 0L;
    private String note;

    @Valid
    private List<ProductOrderDTO> productOrders = new ArrayList<>();

    @Valid
    private List<ServiceBookingDTO> serviceBookings = new ArrayList<>();

        public Long getShopId() {
            return shopId;
        }

        public void setShopId(Long shopId) {
            this.shopId = shopId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public String getReceiverName() {
            return receiverName;
        }

        public void setReceiverName(String receiverName) {
            this.receiverName = receiverName;
        }

        public String getReceiverPhone() {
            return receiverPhone;
        }

        public void setReceiverPhone(String receiverPhone) {
            this.receiverPhone = receiverPhone;
        }

        public String getShippingAddress() {
            return shippingAddress;
        }

        public void setShippingAddress(String shippingAddress) {
            this.shippingAddress = shippingAddress;
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

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public List<ProductOrderDTO> getProductOrders() {
            return productOrders;
        }

        public void setProductOrders(List<ProductOrderDTO> productOrders) {
            this.productOrders = productOrders;
        }

        public List<ServiceBookingDTO> getServiceBookings() {
            return serviceBookings;
        }

        public void setServiceBookings(List<ServiceBookingDTO> serviceBookings) {
            this.serviceBookings = serviceBookings;
        }
}