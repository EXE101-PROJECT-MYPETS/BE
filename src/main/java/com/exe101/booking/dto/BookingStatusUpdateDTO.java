package com.exe101.booking.dto;

import com.exe101.booking.entity.BookingStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingStatusUpdateDTO {
    private BookingStatus status;
}
