package com.exe101.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class BookingCheckoutRequest {

    @Valid
    @NotEmpty(message = "Danh sách dịch vụ hoặc sản phẩm thanh toán không được để trống")
    private List<BookingCheckoutItemRequest> items;

    private OffsetDateTime issuedAt;
}
