package com.exe101.invoice.dto;

import com.exe101.invoice.entity.InvoicePaymentMethod;
import com.exe101.invoice.entity.InvoiceStatus;
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
public class InvoiceDTO {
    private Long id;
    private Long shopId;
    private Long customerId;
    private Long bookingId;
    private Long orderId;
    private Long totalAmount;
    private InvoiceStatus status;
    private InvoicePaymentMethod paymentMethod;
    private OffsetDateTime issuedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<InvoiceLineDTO> lines;
}
