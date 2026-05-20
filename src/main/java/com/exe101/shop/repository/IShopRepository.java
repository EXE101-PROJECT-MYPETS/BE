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
