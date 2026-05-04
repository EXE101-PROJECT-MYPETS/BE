package com.exe101.invoice.repository;

import com.exe101.invoice.entity.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IInvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {
    List<InvoiceLine> findByInvoiceId(Long invoiceId);

    List<InvoiceLine> findByInvoiceIdIn(List<Long> invoiceIds);

    void deleteByInvoiceId(Long invoiceId);
}
