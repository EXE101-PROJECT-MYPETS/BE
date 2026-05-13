package com.exe101.service_shop.repository;

import com.exe101.service_shop.entity.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IServiceRepository extends JpaRepository<Service, Long> {

    @Query("""
            SELECT s
            FROM Service s
            WHERE (:shopId IS NULL OR s.shopId = :shopId)
              AND (:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:categoryId IS NULL OR s.categoryId = :categoryId)
              AND (:active IS NULL OR s.active = :active)
              AND (:cursor IS NULL OR s.id < :cursor)
            ORDER BY s.id DESC
            """)
    List<Service> findForScroll(
            @Param("shopId") Long shopId,
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("active") Boolean active,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("""
            SELECT s
            FROM Service s
            LEFT JOIN ServiceReview r
                ON r.shopId = s.shopId AND r.serviceId = s.id
            WHERE (:shopId IS NULL OR s.shopId = :shopId)
              AND (:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:categoryId IS NULL OR s.categoryId = :categoryId)
              AND (:active IS NULL OR s.active = :active)
              AND (:cursor IS NULL OR s.id < :cursor)
            GROUP BY s
            HAVING (:minRating IS NULL OR COALESCE(AVG(r.rating), 0) >= :minRating)
            ORDER BY COALESCE(AVG(r.rating), 0) DESC, s.id DESC
            """)
    List<Service> findTopRatedForPublic(
            @Param("shopId") Long shopId,
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("active") Boolean active,
            @Param("minRating") Double minRating,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    boolean existsByShopIdAndName(Long shopId, String name);

    boolean existsByShopIdAndNameAndIdNot(Long shopId, String name, Long id);
}
