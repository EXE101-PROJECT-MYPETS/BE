package com.exe101.booking.repository;

import com.exe101.booking.entity.BookingPet;
import com.exe101.booking.entity.BookingPetId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IBookingPetRepository extends JpaRepository<BookingPet, BookingPetId> {
}
