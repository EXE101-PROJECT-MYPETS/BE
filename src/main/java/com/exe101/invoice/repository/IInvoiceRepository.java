package com.exe101.invoice.repository;

import com.exe101.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IInvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByShopIdOrderByIdDesc(Long shopId);

    List<Invoice> findByBookingIdIn(List<Long> bookingIds);

    List<Invoice> findByOrderIdIn(List<Long> orderIds);
}
