package com.exe101.invoice.repository;

import com.exe101.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IInvoiceRepository extends JpaRepository<Invoice, Long> {
}
