package com.exe101.booking.repository;

import com.exe101.booking.entity.Booking;
import com.exe101.booking.entity.BookingSource;
import com.exe101.booking.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface IBookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByIdAndShopId(Long id, Long shopId);

    @Query(
            value = """
                    SELECT b
                    FROM Booking b
                    LEFT JOIN b.customer c
                    LEFT JOIN b.user u
                    WHERE (:shopId IS NULL OR b.shopId = :shopId)
                      AND (:userId IS NULL OR b.userId = :userId)
                      AND (:customerId IS NULL OR b.customerId = :customerId)
                      AND (
                        :customerName IS NULL
                        OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :customerName, '%'))
                        OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :customerName, '%'))
                      )
                      AND (:status IS NULL OR b.status = :status)
                      AND (:source IS NULL OR b.source = :source)
                      AND (:createdFrom IS NULL OR b.createdAt >= :createdFrom)
                      AND (:createdTo IS NULL OR b.createdAt < :createdTo)
                      AND (:appointmentFrom IS NULL OR b.startAt >= :appointmentFrom)
                      AND (:appointmentTo IS NULL OR b.startAt < :appointmentTo)
                    ORDER BY
                      CASE b.status
                        WHEN com.exe101.booking.entity.BookingStatus.DRAFT THEN 1
                        WHEN com.exe101.booking.entity.BookingStatus.CONFIRMED THEN 2
                        WHEN com.exe101.booking.entity.BookingStatus.IN_PROGRESS THEN 3
                        WHEN com.exe101.booking.entity.BookingStatus.COMPLETED THEN 4
                        WHEN com.exe101.booking.entity.BookingStatus.REJECTED THEN 5
                        WHEN com.exe101.booking.entity.BookingStatus.CANCELLED THEN 6
                        ELSE 99
                      END ASC,
                      CASE
                        WHEN b.status IN (
                          com.exe101.booking.entity.BookingStatus.DRAFT,
                          com.exe101.booking.entity.BookingStatus.CONFIRMED,
                          com.exe101.booking.entity.BookingStatus.IN_PROGRESS
                        ) THEN b.startAt
                      END ASC,
                      CASE
                        WHEN b.status IN (
                          com.exe101.booking.entity.BookingStatus.COMPLETED,
                          com.exe101.booking.entity.BookingStatus.REJECTED,
                          com.exe101.booking.entity.BookingStatus.CANCELLED
                        ) THEN b.startAt
                      END DESC,
                      b.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(b)
                    FROM Booking b
                    LEFT JOIN b.customer c
                    LEFT JOIN b.user u
                    WHERE (:shopId IS NULL OR b.shopId = :shopId)
                      AND (:userId IS NULL OR b.userId = :userId)
                      AND (:customerId IS NULL OR b.customerId = :customerId)
                      AND (
                        :customerName IS NULL
                        OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :customerName, '%'))
                        OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :customerName, '%'))
                      )
                      AND (:status IS NULL OR b.status = :status)
                      AND (:source IS NULL OR b.source = :source)
                      AND (:createdFrom IS NULL OR b.createdAt >= :createdFrom)
                      AND (:createdTo IS NULL OR b.createdAt < :createdTo)
                      AND (:appointmentFrom IS NULL OR b.startAt >= :appointmentFrom)
                      AND (:appointmentTo IS NULL OR b.startAt < :appointmentTo)
                    """
    )
    Page<Booking> findForScroll(
            @Param("shopId") Long shopId,
            @Param("userId") Long userId,
            @Param("customerId") Long customerId,
            @Param("customerName") String customerName,
            @Param("status") BookingStatus status,
            @Param("source") BookingSource source,
            @Param("createdFrom") OffsetDateTime createdFrom,
            @Param("createdTo") OffsetDateTime createdTo,
            @Param("appointmentFrom") OffsetDateTime appointmentFrom,
            @Param("appointmentTo") OffsetDateTime appointmentTo,
            Pageable pageable
    );
}
