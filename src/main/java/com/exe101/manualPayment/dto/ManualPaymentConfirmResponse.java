package com.exe101.manualPayment.dto;

import com.exe101.booking.entity.BookingStatus;
import com.exe101.invoice.entity.InvoicePaymentMethod;
import com.exe101.invoice.entity.InvoiceStatus;
import com.exe101.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class ManualPaymentConfirmResponse {

    private Long invoiceId;
    private Long orderId;
    private Long bookingId;
    private Long paidAmount;
    private InvoicePaymentMethod paymentMethod;
    private InvoiceStatus invoiceStatus;
    private OrderStatus orderStatus;
    private BookingStatus bookingStatus;
    private OffsetDateTime confirmedAt;
}
