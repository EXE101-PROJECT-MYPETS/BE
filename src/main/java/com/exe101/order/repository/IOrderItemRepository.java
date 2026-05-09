package com.exe101.order.repository;

import com.exe101.order.entity.OrderItem;
import com.exe101.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IOrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByOrderIdIn(List<Long> orderIds);

    void deleteByOrderId(Long orderId);

    @Query("""
            SELECT COALESCE(SUM(item.qty), 0)
            FROM OrderItem item
            JOIN item.order orderEntity
            WHERE item.shopId = :shopId
              AND item.productId = :productId
              AND orderEntity.status = :status
            """)
    Long sumSoldQtyByShopAndProductIdAndOrderStatus(
            @Param("shopId") Long shopId,
            @Param("productId") Long productId,
            @Param("status") OrderStatus status
    );
}
