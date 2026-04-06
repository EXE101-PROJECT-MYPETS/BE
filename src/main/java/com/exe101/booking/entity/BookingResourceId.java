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
public class BookingResourceId implements Serializable {

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "resource_id")
    private Long resourceId;

    public BookingResourceId() {
    }

    public BookingResourceId(Long bookingId, Long resourceId) {
        this.bookingId = bookingId;
        this.resourceId = resourceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookingResourceId that)) return false;
        return Objects.equals(bookingId, that.bookingId)
                && Objects.equals(resourceId, that.resourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingId, resourceId);
    }
}
