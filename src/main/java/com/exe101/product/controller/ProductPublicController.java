package com.exe101.product.controller;

import com.exe101.common.ScrollResponse;
import com.exe101.product.dto.ProductDTO;
import com.exe101.product.dto.ProductPublicDetailDTO;
import com.exe101.product.dto.ProductPublicReviewDTO;
import com.exe101.product.service.ProductPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/products")
@RequiredArgsConstructor
public class ProductPublicController {

    private final ProductPublicService productPublicService;

    @GetMapping("/mobile")
    public ResponseEntity<ScrollResponse<ProductDTO>> getAllMobile(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopIdHeader,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                productPublicService.getAllForMobile(resolveShopId(shopIdHeader, shopId), keyword, active, cursor, size)
        );
    }

    @GetMapping
    public ResponseEntity<ScrollResponse<ProductDTO>> getByShop(
            @RequestParam Long shopId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(productPublicService.getAllForMobile(shopId, keyword, active, cursor, size));
    }

    @GetMapping("/mobile/{productId}")
    public ResponseEntity<ProductPublicDetailDTO> getMobileProductDetail(@PathVariable Long productId) {
        return ResponseEntity.ok(productPublicService.getMobileProductDetail(productId));
    }

    @GetMapping("/{productId}/related")
    public ResponseEntity<List<ProductDTO>> getRelatedProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(productPublicService.getRelatedProducts(productId, size));
    }

    @GetMapping("/{productId}/reviews")
    public ResponseEntity<List<ProductPublicReviewDTO>> getReviewsByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(productPublicService.getProductReviews(productId, size));
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<ProductPublicReviewDTO>> getReviewsByQuery(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(productPublicService.getProductReviews(productId, size));
    }

    private Long resolveShopId(Long shopIdHeader, Long shopIdQuery) {
        Long resolvedShopId = shopIdHeader != null ? shopIdHeader : shopIdQuery;
        if (resolvedShopId == null) {
            throw new IllegalArgumentException("Thiếu shopId hoặc header X-Shop-Id");
        }
        return resolvedShopId;
    }
}
