package com.exe101.manualPayment.dto;

import com.exe101.invoice.entity.InvoicePaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualPaymentConfirmRequest {

    @NotNull(message = "Mã hóa đơn không được để trống")
    private Long invoiceId;

    private Long orderId;

    private Long bookingId;

    @NotNull(message = "Số tiền thanh toán không được để trống")
    @PositiveOrZero(message = "Số tiền thanh toán phải lớn hơn hoặc bằng 0")
    private Long paidAmount;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private InvoicePaymentMethod paymentMethod;

    @Size(max = 255, message = "Ghi chú không được vượt quá 255 ký tự")
    private String note;
}
