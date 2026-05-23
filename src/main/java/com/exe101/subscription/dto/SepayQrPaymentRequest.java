package com.exe101.subscription.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SepayQrPaymentRequest {

    @NotNull(message = "So thang thanh toan khong duoc de trong")
    private Integer months;
}
