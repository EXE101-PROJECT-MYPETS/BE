package com.exe101.invoice.repository;

import com.exe101.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IInvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByShopIdOrderByIdDesc(Long shopId);

    Optional<Invoice> findByIdAndShopId(Long id, Long shopId);

    Optional<Invoice> findFirstByShopIdAndOrderIdOrderByIdDesc(Long shopId, Long orderId);

    Optional<Invoice> findFirstByShopIdAndBookingIdOrderByIdDesc(Long shopId, Long bookingId);

    List<Invoice> findByBookingIdIn(List<Long> bookingIds);

    List<Invoice> findByOrderIdIn(List<Long> orderIds);
}
