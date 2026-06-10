package com.exe101.subscription.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SepayQrPaymentRequest {

    @NotNull(message = "Số tháng thanh toán không được để trống")
    private Integer months;
}
