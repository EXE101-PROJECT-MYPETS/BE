package com.exe101.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private Long id;
    private Long shopId;
    private Long orderId;
    private Long productId;
    private String productName;
    private Integer qty;
    private Long unitPrice;
    private Long amount;
    private OffsetDateTime createdAt;
}
