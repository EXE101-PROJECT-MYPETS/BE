package com.exe101.payment.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionDTO {
    private Long id;
    private Long shopId;
    private Long paymentIntentId;
    private String providerTxnId;
    private String status;
    private JsonNode rawPayloadJson;
    private OffsetDateTime createdAt;
}
