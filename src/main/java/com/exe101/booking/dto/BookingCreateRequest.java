package com.exe101.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class BookingCreateRequest {

    private Long petId;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private OffsetDateTime startAt;

    private String note;

    @Valid
    @NotEmpty(message = "Danh sách dịch vụ hoặc sản phẩm không được để trống")
    private List<BookingCreateItemRequest> items;
}
