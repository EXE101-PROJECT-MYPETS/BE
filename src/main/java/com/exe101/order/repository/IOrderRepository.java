package com.exe101.order.repository;

import com.exe101.order.entity.CustomerOrder;
import com.exe101.order.entity.OrderSource;
import com.exe101.order.entity.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IOrderRepository extends JpaRepository<CustomerOrder, Long> {

    @Query("""
            SELECT o
            FROM CustomerOrder o
            WHERE (:shopId IS NULL OR o.shopId = :shopId)
              AND (:customerId IS NULL OR o.customerId = :customerId)
              AND (:status IS NULL OR o.status = :status)
              AND (:source IS NULL OR o.source = :source)
              AND (:cursor IS NULL OR o.id < :cursor)
            ORDER BY o.id DESC
            """)
    List<CustomerOrder> findForScroll(
            @Param("shopId") Long shopId,
            @Param("customerId") Long customerId,
            @Param("status") OrderStatus status,
            @Param("source") OrderSource source,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
