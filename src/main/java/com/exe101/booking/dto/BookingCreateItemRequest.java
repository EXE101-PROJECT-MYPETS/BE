package com.exe101.booking.dto;

import com.exe101.booking.entity.BookingItemType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingCreateItemRequest {

    @NotNull(message = "Loại dòng booking không được để trống")
    private BookingItemType itemType;

    @NotNull(message = "Mã dịch vụ hoặc sản phẩm không được để trống")
    private Long refId;

    private Integer qty;
}
