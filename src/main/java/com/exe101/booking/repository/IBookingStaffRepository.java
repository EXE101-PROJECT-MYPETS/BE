package com.exe101.booking.repository;

import com.exe101.booking.dto.BookingStaffDTO;
import com.exe101.booking.entity.BookingStaff;
import com.exe101.booking.entity.BookingStaffId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface IBookingStaffRepository extends JpaRepository<BookingStaff, BookingStaffId> {

    void deleteByBookingId(Long bookingId);

    @Query("""
            SELECT new com.exe101.booking.dto.BookingStaffDTO(
                assignment.bookingId,
                assignment.userId,
                user.fullName,
                user.email,
                user.avatarUrlPreview
            )
            FROM BookingStaff assignment
            JOIN assignment.user user
            WHERE assignment.bookingId IN :bookingIds
            ORDER BY assignment.bookingId ASC, user.fullName ASC, assignment.userId ASC
            """)
    List<BookingStaffDTO> findDisplayByBookingIdIn(@Param("bookingIds") Collection<Long> bookingIds);
}
