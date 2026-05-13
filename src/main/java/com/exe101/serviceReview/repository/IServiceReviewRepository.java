package com.exe101.serviceReview.repository;

import com.exe101.serviceReview.entity.ServiceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IServiceReviewRepository extends JpaRepository<ServiceReview, Long> {

    @Query("""
            SELECT r.serviceId, AVG(r.rating), COUNT(r.id)
            FROM ServiceReview r
            WHERE r.serviceId IN :serviceIds
            GROUP BY r.serviceId
            """)
    List<Object[]> aggregateRatingAndTotalByServiceIds(@Param("serviceIds") List<Long> serviceIds);
}
