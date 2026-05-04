package com.exe101.booking.dto;

import com.exe101.invoice.dto.InvoiceDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookingCheckoutResponse {

    private BookingListItemDTO booking;
    private InvoiceDTO invoice;
}
