package com.exe101.review.repository;

import com.exe101.review.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IReviewRepository extends JpaRepository<Review, Long> {

    @Override
    @EntityGraph(attributePaths = {"customer"})
    List<Review> findAll();

    @Override
    @EntityGraph(attributePaths = {"customer"})
    Optional<Review> findById(Long id);

    @EntityGraph(attributePaths = {"customer"})
    List<Review> findByShopIdOrderByIdDesc(Long shopId);

    @EntityGraph(attributePaths = {"customer"})
    List<Review> findByShopIdAndProductIdOrderByIdDesc(Long shopId, Long productId);

    @EntityGraph(attributePaths = {"customer"})
    List<Review> findByShopIdAndCustomerIdOrderByIdDesc(Long shopId, Long customerId);

    @EntityGraph(attributePaths = {"customer"})
    Optional<Review> findByShopIdAndProductIdAndCustomerId(Long shopId, Long productId, Long customerId);

    @Query("""
            SELECT r.productId, AVG(r.rating), COUNT(r.id)
            FROM Review r
            WHERE r.shopId = :shopId
              AND r.productId IN :productIds
            GROUP BY r.productId
            """)
    List<Object[]> aggregateRatingAndTotalByShopAndProductIds(
            @Param("shopId") Long shopId,
            @Param("productIds") List<Long> productIds
    );

    @Query("""
            SELECT COALESCE(AVG(r.rating), 0), COUNT(r.id)
            FROM Review r
            WHERE r.shopId = :shopId
            """)
    List<Object[]> aggregateRatingAndTotalByShopId(@Param("shopId") Long shopId);

    boolean existsByShopIdAndProductIdAndCustomerId(Long shopId, Long productId, Long customerId);

    boolean existsByShopIdAndProductIdAndCustomerIdAndIdNot(
            Long shopId,
            Long productId,
            Long customerId,
            Long id
    );
}
