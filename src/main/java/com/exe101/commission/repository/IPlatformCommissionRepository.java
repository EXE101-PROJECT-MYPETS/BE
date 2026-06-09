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
