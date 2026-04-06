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
public class BookingPetId implements Serializable {

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "pet_id")
    private Long petId;

    public BookingPetId() {
    }

    public BookingPetId(Long bookingId, Long petId) {
        this.bookingId = bookingId;
        this.petId = petId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookingPetId that)) return false;
        return Objects.equals(bookingId, that.bookingId)
                && Objects.equals(petId, that.petId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingId, petId);
    }
}
