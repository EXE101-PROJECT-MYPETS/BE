package com.exe101.booking.dto;

import com.exe101.booking.entity.BookingItemType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingItemDTO {
    private Long id;
    private Long shopId;
    private Long bookingId;
    private Long petId;
    private BookingItemType itemType;
    private Long refId;
    private Integer qty;
    private Long unitPrice;
    private Long amount;
    private OffsetDateTime createdAt;
}
