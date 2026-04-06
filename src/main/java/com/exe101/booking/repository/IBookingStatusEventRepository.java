package com.exe101.booking.repository;

import com.exe101.booking.entity.BookingStatusEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IBookingStatusEventRepository extends JpaRepository<BookingStatusEvent, Long> {
}
