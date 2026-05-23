package com.exe101.booking.dto;

import com.exe101.booking.entity.BookingItemType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
public class BookingCheckoutItemRequest {

    private Long petId;

    @NotNull(message = "Loại dòng thanh toán không được để trống")
    private BookingItemType itemType;

    private Long refId;

    @NotNull(message = "Số lượng không được để trống")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Integer qty;

    @PositiveOrZero(message = "Đơn giá phải lớn hơn hoặc bằng 0")
    private Long unitPrice;

    private Long veterinarianUserId;
    private String symptoms;
    private String diagnosis;
    private String treatment;
    private String medicalNote;
    private OffsetDateTime followUpAt;
    private LocalDate vaccinatedAt;
    private LocalDate nextDueAt;
    private String clinicName;
    private String vetName;
    private String batchNo;
}
