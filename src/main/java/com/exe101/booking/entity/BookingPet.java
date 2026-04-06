package com.exe101.booking.entity;

import com.exe101.pet.entity.Pet;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "booking_pets")
public class BookingPet {

    @EmbeddedId
    private BookingPetId id;

    @Column(name = "booking_id", nullable = false, insertable = false, updatable = false)
    private Long bookingId;

    @Column(name = "pet_id", nullable = false, insertable = false, updatable = false)
    private Long petId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", insertable = false, updatable = false)
    @JsonIgnore
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", insertable = false, updatable = false)
    @JsonIgnore
    private Pet pet;

    @PrePersist
    public void prePersist() {
        if (id != null) {
            this.bookingId = id.getBookingId();
            this.petId = id.getPetId();
        }
    }
}
