package com.exe101.booking.repository;

import com.exe101.booking.entity.BookingResource;
import com.exe101.booking.entity.BookingResourceId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IBookingResourceRepository extends JpaRepository<BookingResource, BookingResourceId> {
}
