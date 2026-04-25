package com.exe101.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingStaffDTO {
    private Long bookingId;
    private Long userId;
    private String fullName;
    private String email;
    private String avatarUrlPreview;
}
