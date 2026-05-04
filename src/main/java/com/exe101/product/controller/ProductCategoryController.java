package com.exe101.product.controller;

import com.exe101.product.dto.ProductCategoryDTO;
import com.exe101.product.service.ProductCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @GetMapping
    public ResponseEntity<List<ProductCategoryDTO>> getAllByShop(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam(defaultValue = "true") Boolean active
    ) {
        return ResponseEntity.ok(productCategoryService.getAllByShop(shopId, active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductCategoryDTO> getById(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(productCategoryService.getById(shopId, id));
    }

    @PostMapping
    public ResponseEntity<ProductCategoryDTO> create(
            @RequestHeader("X-Shop-Id") Long shopId,
            @Valid @RequestBody ProductCategoryDTO dto
    ) {
        return ResponseEntity.ok(productCategoryService.create(shopId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductCategoryDTO> update(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @Valid @RequestBody ProductCategoryDTO dto
    ) {
        return ResponseEntity.ok(productCategoryService.update(shopId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id
    ) {
        productCategoryService.delete(shopId, id);
        return ResponseEntity.noContent().build();
    }
}
