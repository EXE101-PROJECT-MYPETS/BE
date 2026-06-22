package com.exe101.serviceReview.controller;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.serviceReview.dto.ServiceReviewDTO;
import com.exe101.serviceReview.service.ServiceReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-reviews")
@RequiredArgsConstructor
public class ServiceReviewController {

    private final ServiceReviewService serviceReviewService;

    @GetMapping
    public ResponseEntity<List<ServiceReviewDTO>> getAll(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) Long customerId
    ) {
        return ResponseEntity.ok(serviceReviewService.getAllByShopId(shopId, serviceId, customerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceReviewDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceReviewService.getById(id));
    }

    @PostMapping("/customer")
    public ResponseEntity<ServiceReviewDTO> createCustomerReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ServiceReviewDTO dto
    ) {
        if (principal == null || principal.getUser() == null || principal.getUser().getId() == null) {
            throw new IllegalStateException("Cần đăng nhập để thực hiện chức năng này");
        }
        return ResponseEntity.ok(serviceReviewService.createCustomerReview(principal.getUser().getId(), dto));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        if (principal == null || principal.getUser() == null || principal.getUser().getId() == null) {
            throw new IllegalStateException("Cần đăng nhập để thực hiện chức năng này");
        }
        serviceReviewService.toggleReaction(id, principal.getUser().getId(), true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/dislike")
    public ResponseEntity<Void> dislikeReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        if (principal == null || principal.getUser() == null || principal.getUser().getId() == null) {
            throw new IllegalStateException("Cần đăng nhập để thực hiện chức năng này");
        }
        serviceReviewService.toggleReaction(id, principal.getUser().getId(), false);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/reply")
    public ResponseEntity<ServiceReviewDTO> replyToReview(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @RequestBody(required = false) String replyText
    ) {
        String cleanReply = replyText;
        if (replyText != null && replyText.startsWith("\"") && replyText.endsWith("\"")) {
            cleanReply = replyText.substring(1, replyText.length() - 1);
        }
        return ResponseEntity.ok(serviceReviewService.replyToReview(id, shopId, cleanReply));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceReviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
