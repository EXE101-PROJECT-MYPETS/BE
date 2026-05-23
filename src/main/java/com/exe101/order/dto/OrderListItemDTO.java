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
public class OrderListItemDTO {
    private Long id;
    private String orderCode;
    private Long shopId;
    private String shopName;
    private Long userId;
    private String userFullName;
    private String userPhone;
    private String userEmail;
    private String userAvatarUrlPreview;
    private Long userAddressId;
    private OrderShippingSnapshotDTO shippingSnapshot;
    private List<OrderItemDTO> items;
    private Long totalAmount;
    private OrderStatus status;
    private String statusLabel;
    private OrderSource source;
    private OffsetDateTime createdAt;
}
