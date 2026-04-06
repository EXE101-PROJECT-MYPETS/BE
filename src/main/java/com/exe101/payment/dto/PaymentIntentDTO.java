package com.exe101.payment.dto;

import com.exe101.payment.entity.PaymentIntentStatus;
import com.exe101.payment.entity.PaymentProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentDTO {
    private Long id;
    private Long shopId;
    private Long invoiceId;
    private PaymentProvider provider;
    private String method;
    private Long amount;
    private String currency;
    private PaymentIntentStatus status;
    private OffsetDateTime createdAt;
}
