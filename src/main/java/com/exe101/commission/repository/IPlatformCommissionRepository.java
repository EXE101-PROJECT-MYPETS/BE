package com.exe101.commission.repository;

import com.exe101.commission.entity.CommissionSourceType;
import com.exe101.commission.entity.CommissionStatus;
import com.exe101.commission.entity.PlatformCommission;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface IPlatformCommissionRepository extends JpaRepository<PlatformCommission, Long> {

    Optional<PlatformCommission> findBySourceTypeAndSourceId(CommissionSourceType sourceType, Long sourceId);

    Page<PlatformCommission> findByShopIdOrderByCreatedAtDescIdDesc(Long shopId, Pageable pageable);

    Page<PlatformCommission> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    @Query("""
            SELECT c.shop.id AS shopId,
                   c.shop.name AS shopName,
                   c.shop.imageUrl AS shopImageUrl,
                   COUNT(c.id) AS transactionCount,
                   COUNT(DISTINCT invoice.id) AS invoiceCount,
                   COALESCE(SUM(c.grossAmount), 0) AS grossAmount,
                   COALESCE(SUM(c.commissionBase), 0) AS commissionBase,
                   COALESCE(SUM(c.commissionAmount), 0) AS commissionAmount,
                   COALESCE(SUM(CASE WHEN c.status = :pendingStatus THEN c.commissionAmount ELSE 0 END), 0) AS pendingAmount,
                   COALESCE(SUM(CASE WHEN c.status = :invoicedStatus THEN c.commissionAmount ELSE 0 END), 0) AS invoicedAmount,
                   COALESCE(SUM(CASE WHEN c.status = :collectedStatus THEN c.commissionAmount ELSE 0 END), 0) AS collectedAmount,
                   COALESCE(SUM(CASE
                       WHEN c.status = :invoicedStatus AND invoice.status = :overdueInvoiceStatus
                       THEN c.commissionAmount
                       ELSE 0
                   END), 0) AS overdueAmount
            FROM PlatformCommission c
            LEFT JOIN PlatformCommissionInvoiceItem item ON item.commissionId = c.id
            LEFT JOIN PlatformCommissionInvoice invoice ON invoice.id = item.invoiceId
            WHERE c.createdAt >= :from
              AND c.createdAt < :toExclusive
              AND c.status NOT IN :excludedStatuses
            GROUP BY c.shop.id, c.shop.name, c.shop.imageUrl
            ORDER BY COALESCE(SUM(c.commissionAmount), 0) DESC, c.shop.name ASC, c.shop.id ASC
            """)
    List<AdminShopMonthlyCommissionProjection> findAdminMonthlyCommissionRows(
            @Param("from") OffsetDateTime from,
            @Param("toExclusive") OffsetDateTime toExclusive,
            @Param("pendingStatus") CommissionStatus pendingStatus,
            @Param("invoicedStatus") CommissionStatus invoicedStatus,
            @Param("collectedStatus") CommissionStatus collectedStatus,
            @Param("overdueInvoiceStatus") com.exe101.commission.entity.CommissionInvoiceStatus overdueInvoiceStatus,
            @Param("excludedStatuses") List<CommissionStatus> excludedStatuses
    );

    @Query("""
            SELECT c
            FROM PlatformCommission c
            WHERE c.shopId = :shopId
              AND c.createdAt >= :from
              AND c.createdAt < :toExclusive
              AND c.status NOT IN :excludedStatuses
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    List<PlatformCommission> findAdminShopMonthlyCommissions(
            @Param("shopId") Long shopId,
            @Param("from") OffsetDateTime from,
            @Param("toExclusive") OffsetDateTime toExclusive,
            @Param("excludedStatuses") List<CommissionStatus> excludedStatuses
    );

    @Query("""
            SELECT c
            FROM PlatformCommission c
            WHERE c.shopId = :shopId
              AND (cast(:status as string) IS NULL OR c.status = :status)
              AND (cast(:sourceType as string) IS NULL OR c.sourceType = :sourceType)
              AND (cast(:from as string) IS NULL OR c.createdAt >= :from)
              AND (cast(:to as string) IS NULL OR c.createdAt < :to)
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    Page<PlatformCommission> findShopCommissionsForFilter(
            @Param("shopId") Long shopId,
            @Param("status") CommissionStatus status,
            @Param("sourceType") CommissionSourceType sourceType,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c
            FROM PlatformCommission c
            WHERE c.shopId = :shopId
              AND c.status = :status
              AND c.createdAt >= :from
              AND c.createdAt < :toExclusive
            ORDER BY c.createdAt ASC, c.id ASC
            """)
    List<PlatformCommission> findByShopIdAndStatusInPeriodForUpdate(
            @Param("shopId") Long shopId,
            @Param("status") CommissionStatus status,
            @Param("from") OffsetDateTime from,
            @Param("toExclusive") OffsetDateTime toExclusive
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c
            FROM PlatformCommission c
            WHERE c.shopId = :shopId
              AND c.status = :status
              AND c.createdAt < :toExclusive
            ORDER BY c.createdAt ASC, c.id ASC
            """)
    List<PlatformCommission> findByShopIdAndStatusBeforeForUpdate(
            @Param("shopId") Long shopId,
            @Param("status") CommissionStatus status,
            @Param("toExclusive") OffsetDateTime toExclusive
    );

    @Query("""
            SELECT DISTINCT c.shopId
            FROM PlatformCommission c
            WHERE c.status = :status
              AND c.createdAt >= :from
              AND c.createdAt < :toExclusive
            ORDER BY c.shopId ASC
            """)
    List<Long> findShopIdsWithStatusInPeriod(
            @Param("status") CommissionStatus status,
            @Param("from") OffsetDateTime from,
            @Param("toExclusive") OffsetDateTime toExclusive
    );

    @Query("""
            SELECT DISTINCT c.shopId
            FROM PlatformCommission c
            WHERE c.status = :status
              AND c.createdAt < :toExclusive
            ORDER BY c.shopId ASC
            """)
    List<Long> findShopIdsWithStatusBefore(
            @Param("status") CommissionStatus status,
            @Param("toExclusive") OffsetDateTime toExclusive
    );

    @Query("""
            SELECT COALESCE(SUM(c.commissionAmount), 0)
            FROM PlatformCommission c
            WHERE c.shopId = :shopId
              AND c.status IN :statuses
            """)
    Long sumCommissionAmountByShopIdAndStatusIn(
            @Param("shopId") Long shopId,
            @Param("statuses") List<CommissionStatus> statuses
    );

    long countByShopIdAndStatus(Long shopId, CommissionStatus status);

    @Modifying
    @Query("""
            UPDATE PlatformCommission c
            SET c.status = :status,
                c.invoicedAt = :invoicedAt
            WHERE c.id IN :ids
            """)
    int markInvoiced(
            @Param("ids") List<Long> ids,
            @Param("status") CommissionStatus status,
            @Param("invoicedAt") OffsetDateTime invoicedAt
    );

    @Modifying
    @Query("""
            UPDATE PlatformCommission c
            SET c.status = :status,
                c.collectedAt = :collectedAt
            WHERE c.id IN :ids
            """)
    int markCollected(
            @Param("ids") List<Long> ids,
            @Param("status") CommissionStatus status,
            @Param("collectedAt") OffsetDateTime collectedAt
    );
}
