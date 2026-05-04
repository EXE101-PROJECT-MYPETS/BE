package com.exe101.product.controller;

import com.exe101.common.ScrollResponse;
import com.exe101.product.dto.ProductCreateRequest;
import com.exe101.product.dto.ProductDTO;
import com.exe101.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ScrollResponse<ProductDTO>> getAll(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(productService.getAllForScroll(shopId, keyword, active, cursor, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDTO> createMultipart(
            @RequestHeader("X-Shop-Id") Long shopId,
            @ModelAttribute @Valid ProductCreateRequest request
    ) {
        return ResponseEntity.ok(productService.create(shopId, request));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductDTO> update(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @Valid @RequestBody ProductDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(productService.update(id, dto));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDTO> updateMultipart(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @ModelAttribute @Valid ProductCreateRequest request
    ) {
        return ResponseEntity.ok(productService.update(id, shopId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
