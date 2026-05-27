package com.exe101.order.dto;

import com.exe101.order.entity.OrderCancelRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelRequestDTO {
    private Long id;
    private Long shopId;
    private Long orderId;
    private Long userId;
    private String reason;
    private OrderCancelRequestStatus status;
    private Long reviewedBy;
    private String reviewNote;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
