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
public class BookingListItemDTO {

    private Long id;
    private String bookingCode;
    private Long shopId;
    private Long userId;
    private Long customerId;
    private Long petId;
    private String userFullName;
    private String userPhone;
    private String userEmail;
    private String userAvatarUrlPreview;
    private String customerFullName;
    private String customerPhone;
    private String customerEmail;
    private String petName;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private List<BookingLineItemDTO> items;
    private Long totalAmount;
    private BookingStatus status;
    private String statusLabel;
    private BookingSource source;
    private String note;
    private Long createdBy;
    private OffsetDateTime time;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
