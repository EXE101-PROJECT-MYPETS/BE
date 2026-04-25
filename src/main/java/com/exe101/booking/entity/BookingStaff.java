package com.exe101.booking.entity;

import com.exe101.shopMember.entity.ShopMember;
import com.exe101.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "booking_staff")
public class BookingStaff {

    @EmbeddedId
    private BookingStaffId id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "booking_id", nullable = false, insertable = false, updatable = false)
    private Long bookingId;

    @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", insertable = false, updatable = false)
    @JsonIgnore
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "shop_id", referencedColumnName = "shop_id", insertable = false, updatable = false),
            @JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    })
    @JsonIgnore
    private ShopMember shopMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnore
    private User user;

    @PrePersist
    @PreUpdate
    public void syncKeys() {
        if (id != null) {
            this.bookingId = id.getBookingId();
            this.userId = id.getUserId();
        }
    }
}
