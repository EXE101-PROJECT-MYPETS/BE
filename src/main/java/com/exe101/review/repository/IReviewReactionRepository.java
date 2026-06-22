package com.exe101.review.repository;

import com.exe101.review.entity.ReviewReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface IReviewReactionRepository extends JpaRepository<ReviewReaction, Long> {
    Optional<ReviewReaction> findByReviewIdAndUserId(Long reviewId, Long userId);
    @Query("select count(rr) from ReviewReaction rr where rr.reviewId = :reviewId and rr.isLike = :isLike")
    long countByReviewIdAndIsLike(@Param("reviewId") Long reviewId, @Param("isLike") Boolean isLike);
    List<ReviewReaction> findByReviewIdInAndUserId(List<Long> reviewIds, Long userId);
}
