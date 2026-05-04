package com.exe101.product.repository;

import com.exe101.product.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByShopIdOrderByIdDesc(Long shopId);

    boolean existsByShopIdAndSku(Long shopId, String sku);

    boolean existsByShopIdAndSkuAndIdNot(Long shopId, String sku, Long id);

    @Query("""
            SELECT p
            FROM Product p
            WHERE p.shopId = :shopId
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:active IS NULL OR p.active = :active)
              AND (:cursor IS NULL OR p.id < :cursor)
            ORDER BY p.id DESC
            """)
    List<Product> findForScroll(
            @Param("shopId") Long shopId,
            @Param("keyword") String keyword,
            @Param("active") Boolean active,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
