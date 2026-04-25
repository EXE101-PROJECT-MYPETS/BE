package com.exe101.invoice.dto;

import com.exe101.invoice.entity.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDTO {
    private Long id;
    private Long shopId;
    private Long customerId;
    private Long bookingId;
    private Long orderId;
    private Long totalAmount;
    private InvoiceStatus status;
    private OffsetDateTime issuedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
