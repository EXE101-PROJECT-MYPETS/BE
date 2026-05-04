package com.exe101.inventory.repository;

import com.exe101.inventory.entity.Inventory;
import com.exe101.inventory.entity.InventoryId;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IInventoryRepository extends JpaRepository<Inventory, InventoryId> {
    List<Inventory> findByShopId(Long shopId);

    List<Inventory> findByShopIdAndProductIdIn(Long shopId, List<Long> productIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i
            FROM Inventory i
            WHERE i.shopId = :shopId
              AND i.productId IN :productIds
            """)
    List<Inventory> findByShopIdAndProductIdInForUpdate(
            @Param("shopId") Long shopId,
            @Param("productIds") List<Long> productIds
    );

    @Query("""
            SELECT i
            FROM Inventory i
            WHERE i.shopId = :shopId
              AND (:cursor IS NULL OR i.productId > :cursor)
            ORDER BY i.productId ASC
            """)
    List<Inventory> findForScroll(
            @Param("shopId") Long shopId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
