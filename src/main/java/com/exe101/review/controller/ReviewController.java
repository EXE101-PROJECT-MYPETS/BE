package com.exe101.review.controller;

import com.exe101.review.dto.ReviewDTO;
import com.exe101.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<ReviewDTO>> getAll(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long customerId
    ) {
        return ResponseEntity.ok(reviewService.getAllByShopId(shopId, productId, customerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ReviewDTO> create(
            @RequestHeader("X-Shop-Id") Long shopId,
            @Valid @RequestBody ReviewDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(reviewService.create(dto));
    }

    @PostMapping("/customer")
    public ResponseEntity<ReviewDTO> createCustomerReview(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.exe101.auth.model.UserPrincipal principal,
            @Valid @RequestBody ReviewDTO dto
    ) {
        if (principal == null || principal.getUser() == null || principal.getUser().getId() == null) {
            throw new IllegalStateException("Cần đăng nhập để thực hiện chức năng này");
        }
        return ResponseEntity.ok(reviewService.createCustomerReview(principal.getUser().getId(), dto));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeReview(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.exe101.auth.model.UserPrincipal principal,
            @PathVariable Long id
    ) {
        if (principal == null || principal.getUser() == null || principal.getUser().getId() == null) {
            throw new IllegalStateException("Cần đăng nhập để thực hiện chức năng này");
        }
        reviewService.toggleReaction(id, principal.getUser().getId(), true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/dislike")
    public ResponseEntity<Void> dislikeReview(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.exe101.auth.model.UserPrincipal principal,
            @PathVariable Long id
    ) {
        if (principal == null || principal.getUser() == null || principal.getUser().getId() == null) {
            throw new IllegalStateException("Cần đăng nhập để thực hiện chức năng này");
        }
        reviewService.toggleReaction(id, principal.getUser().getId(), false);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/reply")
    public ResponseEntity<ReviewDTO> replyToReview(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @RequestBody(required = false) String replyText
    ) {
        String cleanReply = replyText;
        if (replyText != null && replyText.startsWith("\"") && replyText.endsWith("\"")) {
            cleanReply = replyText.substring(1, replyText.length() - 1);
        }
        return ResponseEntity.ok(reviewService.replyToReview(id, shopId, cleanReply));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewDTO> update(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @Valid @RequestBody ReviewDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(reviewService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
