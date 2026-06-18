package com.exe101.shop.repository;

import com.exe101.shop.entity.Shop;
import com.exe101.shop.entity.ShopStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IShopRepository extends JpaRepository<Shop, Long> {

    List<Shop> findAllByOrderByIdAsc();

    List<Shop> findAllByStatusOrderByIdAsc(ShopStatus status);

    Optional<Shop> findByIdAndStatus(Long id, ShopStatus status);

    @Query(value = """
            SELECT
                s.id,
                s.name,
                s.image_url,
                s.cover_image_url,
                (COALESCE(pr.avg_rating, 0) + COALESCE(sr.avg_rating, 0)) / 2.0 AS rating,
                COALESCE(pc.product_count, 0) AS product_count,
                COALESCE(sc.service_count, 0) AS service_count,
                s.address_text,
                s.lat,
                s.lng,
                (
                    6371 * ACOS(
                        LEAST(
                            1,
                            GREATEST(
                                -1,
                                COS(RADIANS(:lat)) * COS(RADIANS(s.lat))
                                * COS(RADIANS(s.lng) - RADIANS(:lng))
                                + SIN(RADIANS(:lat)) * SIN(RADIANS(s.lat))
                            )
                        )
                    )
                ) AS distance_km,
                s.opening_hours,
                s.closing_hours
            FROM prod.shops s
            LEFT JOIN (
                SELECT r.shop_id, AVG(r.rating) AS avg_rating
                FROM prod.reviews r
                GROUP BY r.shop_id
            ) pr ON pr.shop_id = s.id
            LEFT JOIN (
                SELECT r.shop_id, AVG(r.rating) AS avg_rating
                FROM prod.service_reviews r
                GROUP BY r.shop_id
            ) sr ON sr.shop_id = s.id
            LEFT JOIN (
                SELECT p.shop_id, COUNT(*) AS product_count
                FROM prod.products p
                WHERE p.active = true
                GROUP BY p.shop_id
            ) pc ON pc.shop_id = s.id
            LEFT JOIN (
                SELECT sv.shop_id, COUNT(*) AS service_count
                FROM prod.services sv
                WHERE sv.active = true
                GROUP BY sv.shop_id
            ) sc ON sc.shop_id = s.id
            WHERE s.status = 'ACTIVE'
              AND (
                  :radiusKm IS NULL OR (
                      6371 * ACOS(
                          LEAST(
                              1,
                              GREATEST(
                                  -1,
                                  COS(RADIANS(:lat)) * COS(RADIANS(s.lat))
                                  * COS(RADIANS(s.lng) - RADIANS(:lng))
                                  + SIN(RADIANS(:lat)) * SIN(RADIANS(s.lat))
                              )
                          )
                      )
                  ) <= :radiusKm
              )
            ORDER BY distance_km ASC, s.id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findNearbyShopRows(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radiusKm") Double radiusKm,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT s.id
            FROM prod.shops s
            WHERE (
                6371 * ACOS(
                    LEAST(
                        1,
                        GREATEST(
                            -1,
                            COS(RADIANS(:lat)) * COS(RADIANS(s.lat))
                            * COS(RADIANS(s.lng) - RADIANS(:lng))
                            + SIN(RADIANS(:lat)) * SIN(RADIANS(s.lat))
                        )
                    )
                )
            ) <= :radiusKm
            ORDER BY (
                6371 * ACOS(
                    LEAST(
                        1,
                        GREATEST(
                            -1,
                            COS(RADIANS(:lat)) * COS(RADIANS(s.lat))
                            * COS(RADIANS(s.lng) - RADIANS(:lng))
                            + SIN(RADIANS(:lat)) * SIN(RADIANS(s.lat))
                        )
                    )
                )
            ) ASC, s.id ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Long> findNearestShopIdWithinRadius(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radiusKm") Double radiusKm
    );

    @Query(value = """
            SELECT s.id
            FROM prod.shops s
            ORDER BY (
                6371 * ACOS(
                    LEAST(
                        1,
                        GREATEST(
                            -1,
                            COS(RADIANS(:lat)) * COS(RADIANS(s.lat))
                            * COS(RADIANS(s.lng) - RADIANS(:lng))
                            + SIN(RADIANS(:lat)) * SIN(RADIANS(s.lat))
                        )
                    )
                )
            ) ASC, s.id ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Long> findNearestShopId(
            @Param("lat") Double lat,
            @Param("lng") Double lng
    );
}
