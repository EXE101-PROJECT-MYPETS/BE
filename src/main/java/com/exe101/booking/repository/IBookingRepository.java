package com.exe101.booking.repository;

import com.exe101.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IBookingRepository extends JpaRepository<Booking, Long> {
}
