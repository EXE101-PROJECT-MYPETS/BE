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
