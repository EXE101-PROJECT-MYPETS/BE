package com.exe101.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class BookingStaffId implements Serializable {

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "user_id")
    private Long userId;

    public BookingStaffId() {
    }

    public BookingStaffId(Long bookingId, Long userId) {
        this.bookingId = bookingId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookingStaffId that)) return false;
        return Objects.equals(bookingId, that.bookingId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingId, userId);
    }
}
