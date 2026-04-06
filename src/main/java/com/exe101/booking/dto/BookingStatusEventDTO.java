package com.exe101.booking.dto;

import com.exe101.booking.entity.BookingStatus;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatusEventDTO {
    private Long id;
    private Long shopId;
    private Long bookingId;
    private BookingStatus fromStatus;
    private BookingStatus toStatus;
    private Long actorUserId;
    private JsonNode metaJson;
    private OffsetDateTime createdAt;
}
