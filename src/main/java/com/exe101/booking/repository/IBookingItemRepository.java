package com.exe101.booking.repository;

import com.exe101.booking.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IBookingItemRepository extends JpaRepository<BookingItem, Long> {

    List<BookingItem> findByBookingId(Long bookingId);

    List<BookingItem> findByBookingIdIn(List<Long> bookingIds);

    void deleteByBookingId(Long bookingId);
}
