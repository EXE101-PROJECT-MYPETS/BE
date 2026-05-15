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
              AND (:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
              AND (:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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

    @Query(value = """
            WITH aggregated AS (
                SELECT
                    s.id,
                    s.shop_id,
                    COALESCE(AVG(sr.rating), 0) AS avg_rating,
                    (
                        6371 * ACOS(
                            LEAST(
                                1,
                                GREATEST(
                                    -1,
                                    COS(RADIANS(:lat)) * COS(RADIANS(sh.lat))
                                    * COS(RADIANS(sh.lng) - RADIANS(:lng))
                                    + SIN(RADIANS(:lat)) * SIN(RADIANS(sh.lat))
                                )
                            )
                        )
                    ) AS distance_km
                FROM prod.services s
                JOIN prod.shops sh
                    ON sh.id = s.shop_id
                LEFT JOIN prod.service_reviews sr
                    ON sr.shop_id = s.shop_id
                   AND sr.service_id = s.id
                WHERE (:shopId IS NULL OR s.shop_id = :shopId)
                  AND (:search IS NULL OR LOWER(s.name) LIKE CONCAT('%', LOWER(:search), '%'))
                  AND (:categoryId IS NULL OR s.category_id = :categoryId)
                  AND (:active IS NULL OR s.active = :active)
                  AND (:cursor IS NULL OR s.id < :cursor)
                GROUP BY s.id, s.shop_id, sh.lat, sh.lng
                HAVING (:minRating IS NULL OR COALESCE(AVG(sr.rating), 0) >= :minRating)
            ),
            ranked AS (
                SELECT
                    a.id,
                    a.shop_id,
                    a.distance_km,
                    a.avg_rating,
                    ROW_NUMBER() OVER (
                        PARTITION BY a.shop_id
                        ORDER BY a.avg_rating DESC, a.id DESC
                    ) AS shop_rank
                FROM aggregated a
            )
            SELECT r.id
            FROM ranked r
            WHERE r.shop_rank <= :perShopLimit
            ORDER BY r.shop_rank ASC, r.distance_km ASC, r.avg_rating DESC, r.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findIdsForPublicOrderByDistanceThenRating(
            @Param("shopId") Long shopId,
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("active") Boolean active,
            @Param("minRating") Double minRating,
            @Param("cursor") Long cursor,
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("perShopLimit") int perShopLimit,
            @Param("limit") int limit
    );

    boolean existsByShopIdAndName(Long shopId, String name);

    boolean existsByShopIdAndNameAndIdNot(Long shopId, String name, Long id);
}
