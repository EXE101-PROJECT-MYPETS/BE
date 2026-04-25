package com.exe101.order.dto;

import com.exe101.order.entity.OrderSource;
import com.exe101.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private String orderCode;
    private Long shopId;
    private Long customerId;
    private OrderStatus status;
    private OrderSource source;
    private Long subtotalAmount;
    private Long shippingFee;
    private Long discountAmount;
    private Long totalAmount;
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;
    private String note;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<OrderItemDTO> items;
}
