package com.exe101.commission.repository;

import com.exe101.commission.entity.PlatformCommissionInvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IPlatformCommissionInvoiceItemRepository extends JpaRepository<PlatformCommissionInvoiceItem, Long> {

    List<PlatformCommissionInvoiceItem> findByInvoiceIdOrderByIdAsc(Long invoiceId);

    List<PlatformCommissionInvoiceItem> findByCommissionIdIn(List<Long> commissionIds);

    @Query("""
            SELECT item.commissionId
            FROM PlatformCommissionInvoiceItem item
            WHERE item.invoiceId = :invoiceId
            """)
    List<Long> findCommissionIdsByInvoiceId(@Param("invoiceId") Long invoiceId);
}
