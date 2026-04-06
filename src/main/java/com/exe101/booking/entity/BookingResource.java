package com.exe101.booking.entity;

import com.exe101.resource.entity.ShopResource;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "booking_resources")
public class BookingResource {

    @EmbeddedId
    private BookingResourceId id;

    @Column(name = "booking_id", nullable = false, insertable = false, updatable = false)
    private Long bookingId;

    @Column(name = "resource_id", nullable = false, insertable = false, updatable = false)
    private Long resourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", insertable = false, updatable = false)
    @JsonIgnore
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", insertable = false, updatable = false)
    @JsonIgnore
    private ShopResource resource;

    @PrePersist
    public void prePersist() {
        if (id != null) {
            this.bookingId = id.getBookingId();
            this.resourceId = id.getResourceId();
        }
    }
}
