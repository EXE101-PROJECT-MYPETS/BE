package com.exe101.invoice.repository;

import com.exe101.invoice.entity.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IInvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {
}
