package com.exe101.order.repository;

import com.exe101.order.entity.CustomerOrder;
import com.exe101.order.entity.OrderSource;
import com.exe101.order.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface IOrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByIdAndShopId(Long id, Long shopId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o
            FROM CustomerOrder o
            WHERE o.id = :id
              AND o.shopId = :shopId
            """)
    Optional<CustomerOrder> findByIdAndShopIdForUpdate(
            @Param("id") Long id,
            @Param("shopId") Long shopId
    );

    @Query("""
            SELECT o
            FROM CustomerOrder o
            WHERE (cast(:shopId as string) IS NULL OR o.shopId = :shopId)
              AND (cast(:userId as string) IS NULL OR o.userId = :userId)
              AND (cast(:customerId as string) IS NULL OR o.customerId = :customerId)
              AND (cast(:status as string) IS NULL OR o.status = :status)
              AND (cast(:source as string) IS NULL OR o.source = :source)
              AND (cast(:createdFrom as string) IS NULL OR o.createdAt >= :createdFrom)
              AND (cast(:createdTo as string) IS NULL OR o.createdAt < :createdTo)
              AND (
                cast(:cursorId as string) IS NULL
                OR (
                  CASE
                    WHEN o.status = :pendingStatus THEN 0
                    WHEN o.status = :confirmedStatus THEN 1
                    WHEN o.status = :packingStatus THEN 2
                    WHEN o.status = :waitingGhtkPickupStatus THEN 3
                    WHEN o.status = :shippingStatus THEN 4
                    WHEN o.status = :completedStatus THEN 5
                    WHEN o.status = :cancelledStatus THEN 6
                    ELSE 7
                  END
                ) > :cursorPriority
                OR (
                  (
                    CASE
                      WHEN o.status = :pendingStatus THEN 0
                      WHEN o.status = :confirmedStatus THEN 1
                      WHEN o.status = :packingStatus THEN 2
                      WHEN o.status = :waitingGhtkPickupStatus THEN 3
                      WHEN o.status = :shippingStatus THEN 4
                      WHEN o.status = :completedStatus THEN 5
                      WHEN o.status = :cancelledStatus THEN 6
                      ELSE 7
                    END
                  ) = :cursorPriority
                  AND (
                    o.createdAt < :cursorCreatedAt
                    OR (o.createdAt = :cursorCreatedAt AND o.id < :cursorId)
                  )
                )
              )
            ORDER BY
              CASE
                WHEN o.status = :pendingStatus THEN 0
                WHEN o.status = :confirmedStatus THEN 1
                WHEN o.status = :packingStatus THEN 2
                WHEN o.status = :waitingGhtkPickupStatus THEN 3
                WHEN o.status = :shippingStatus THEN 4
                WHEN o.status = :completedStatus THEN 5
                WHEN o.status = :cancelledStatus THEN 6
                ELSE 7
              END ASC,
              o.createdAt DESC,
              o.id DESC
            """)
    List<CustomerOrder> findForScroll(
            @Param("shopId") Long shopId,
            @Param("userId") Long userId,
            @Param("customerId") Long customerId,
            @Param("status") OrderStatus status,
            @Param("source") OrderSource source,
            @Param("createdFrom") OffsetDateTime createdFrom,
            @Param("createdTo") OffsetDateTime createdTo,
            @Param("cursorId") Long cursorId,
            @Param("cursorPriority") Integer cursorPriority,
            @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
            @Param("pendingStatus") OrderStatus pendingStatus,
            @Param("confirmedStatus") OrderStatus confirmedStatus,
            @Param("packingStatus") OrderStatus packingStatus,
            @Param("waitingGhtkPickupStatus") OrderStatus waitingGhtkPickupStatus,
            @Param("shippingStatus") OrderStatus shippingStatus,
            @Param("completedStatus") OrderStatus completedStatus,
            @Param("cancelledStatus") OrderStatus cancelledStatus,
            Pageable pageable
    );
}
