package com.exe101.serviceReview.repository;

import com.exe101.serviceReview.entity.ServiceReviewReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface IServiceReviewReactionRepository extends JpaRepository<ServiceReviewReaction, Long> {
    Optional<ServiceReviewReaction> findByServiceReviewIdAndUserId(Long serviceReviewId, Long userId);
    @Query("select count(rr) from ServiceReviewReaction rr where rr.serviceReviewId = :serviceReviewId and rr.isLike = :isLike")
    long countByServiceReviewIdAndIsLike(@Param("serviceReviewId") Long serviceReviewId, @Param("isLike") Boolean isLike);
    List<ServiceReviewReaction> findByServiceReviewIdInAndUserId(List<Long> serviceReviewIds, Long userId);
}
