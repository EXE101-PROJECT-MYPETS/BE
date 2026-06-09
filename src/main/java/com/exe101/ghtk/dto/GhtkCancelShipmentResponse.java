package com.exe101.ghtk.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GhtkCancelShipmentResponse {
    private boolean success;
    private String message;
    private Long orderId;
    private String labelId;
    private String partnerId;
    private String shippingStatus;
    private String orderStatus;
}
