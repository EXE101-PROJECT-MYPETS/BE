package com.exe101.commission.repository;

import com.exe101.commission.entity.CommissionInvoiceStatus;
import com.exe101.commission.entity.PlatformCommissionInvoice;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface IPlatformCommissionInvoiceRepository extends JpaRepository<PlatformCommissionInvoice, Long> {

    Optional<PlatformCommissionInvoice> findByShopIdAndPeriodFromAndPeriodTo(
            Long shopId,
            LocalDate periodFrom,
            LocalDate periodTo
    );

    Optional<PlatformCommissionInvoice> findByShopIdAndId(Long shopId, Long id);

    Page<PlatformCommissionInvoice> findByShopIdOrderByCreatedAtDescIdDesc(Long shopId, Pageable pageable);

    Page<PlatformCommissionInvoice> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    @Query("""
            SELECT DISTINCT item.invoice
            FROM PlatformCommissionInvoiceItem item
            JOIN item.commission commission
            WHERE commission.shopId = :shopId
              AND commission.createdAt >= :from
              AND commission.createdAt < :toExclusive
              AND commission.status NOT IN :excludedStatuses
            ORDER BY item.invoice.createdAt DESC, item.invoice.id DESC
            """)
    List<PlatformCommissionInvoice> findAdminShopMonthlyInvoices(
            @Param("shopId") Long shopId,
            @Param("from") OffsetDateTime from,
            @Param("toExclusive") OffsetDateTime toExclusive,
            @Param("excludedStatuses") List<com.exe101.commission.entity.CommissionStatus> excludedStatuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i
            FROM PlatformCommissionInvoice i
            WHERE i.id = :id
            """)
    Optional<PlatformCommissionInvoice> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT i
            FROM PlatformCommissionInvoice i
            WHERE i.status IN :statuses
              AND i.dueAt < :now
            """)
    List<PlatformCommissionInvoice> findOverdueCandidates(
            @Param("statuses") List<CommissionInvoiceStatus> statuses,
            @Param("now") OffsetDateTime now
    );

    @Query("""
            SELECT i
            FROM PlatformCommissionInvoice i
            WHERE i.shopId = :shopId
              AND i.status IN :statuses
            ORDER BY i.dueAt ASC, i.id ASC
            """)
    List<PlatformCommissionInvoice> findUnpaidByShopId(
            @Param("shopId") Long shopId,
            @Param("statuses") List<CommissionInvoiceStatus> statuses
    );

    @Query("""
            SELECT COALESCE(SUM(i.totalCommissionAmount), 0)
            FROM PlatformCommissionInvoice i
            WHERE i.shopId = :shopId
              AND i.status IN :statuses
            """)
    Long sumUnpaidAmountByShopId(
            @Param("shopId") Long shopId,
            @Param("statuses") List<CommissionInvoiceStatus> statuses
    );

    long countByShopIdAndStatusIn(Long shopId, List<CommissionInvoiceStatus> statuses);

    long countByShopIdAndStatus(Long shopId, CommissionInvoiceStatus status);

    @Query("""
            SELECT COALESCE(SUM(i.totalCommissionAmount), 0)
            FROM PlatformCommissionInvoice i
            WHERE i.shopId = :shopId
              AND i.status = :status
            """)
    Long sumAmountByShopIdAndStatus(
            @Param("shopId") Long shopId,
            @Param("status") CommissionInvoiceStatus status
    );

    @Query(value = """
            SELECT *
            FROM platform_commission_invoices i
            WHERE :content ILIKE CONCAT('%', i.transfer_content, '%')
               OR :content ILIKE CONCAT('%', i.invoice_code, '%')
               OR regexp_replace(:content, '[^A-Za-z0-9]', '', 'g')
                    ILIKE CONCAT('%', regexp_replace(i.transfer_content, '[^A-Za-z0-9]', '', 'g'), '%')
               OR regexp_replace(:content, '[^A-Za-z0-9]', '', 'g')
                    ILIKE CONCAT('%', regexp_replace(i.invoice_code, '[^A-Za-z0-9]', '', 'g'), '%')
            ORDER BY i.created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<PlatformCommissionInvoice> findLatestBySearchableContent(@Param("content") String content);

    @Modifying
    @Query("""
            UPDATE PlatformCommissionInvoice i
            SET i.status = :status
            WHERE i.id IN :ids
            """)
    int updateStatusByIds(
            @Param("ids") List<Long> ids,
            @Param("status") CommissionInvoiceStatus status
    );
}
