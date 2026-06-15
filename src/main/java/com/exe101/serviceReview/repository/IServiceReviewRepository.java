package com.exe101.serviceReview.repository;

import com.exe101.serviceReview.entity.ServiceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IServiceReviewRepository extends JpaRepository<ServiceReview, Long> {

    @Query("""
            SELECT r
            FROM ServiceReview r
            LEFT JOIN FETCH r.customer
            WHERE r.shopId = :shopId
              AND r.serviceId = :serviceId
            ORDER BY r.id DESC
            """)
    List<ServiceReview> findByShopIdAndServiceIdOrderByIdDesc(
            @Param("shopId") Long shopId,
            @Param("serviceId") Long serviceId
    );

    @Query("""
            SELECT r.serviceId, AVG(r.rating), COUNT(r.id)
            FROM ServiceReview r
            WHERE r.serviceId IN :serviceIds
            GROUP BY r.serviceId
            """)
    List<Object[]> aggregateRatingAndTotalByServiceIds(@Param("serviceIds") List<Long> serviceIds);

    @Query("""
            SELECT COALESCE(AVG(r.rating), 0), COUNT(r.id)
            FROM ServiceReview r
            WHERE r.shopId = :shopId
            """)
    List<Object[]> aggregateRatingAndTotalByShopId(@Param("shopId") Long shopId);

    @Query("""
            SELECT r.shopId, COALESCE(AVG(r.rating), 0)
            FROM ServiceReview r
            WHERE r.shopId IN :shopIds
            GROUP BY r.shopId
            """)
    List<Object[]> aggregateRatingByShopIds(@Param("shopIds") List<Long> shopIds);
}
