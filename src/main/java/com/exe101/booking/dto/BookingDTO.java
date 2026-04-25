package com.exe101.booking.dto;

import com.exe101.booking.entity.BookingSource;
import com.exe101.booking.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {
    private Long id;
    private Long shopId;
    private Long customerId;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private BookingStatus status;
    private BookingSource source;
    private String note;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<Long> assignedStaffIds;
    private List<BookingStaffDTO> assignedStaffs;
}
