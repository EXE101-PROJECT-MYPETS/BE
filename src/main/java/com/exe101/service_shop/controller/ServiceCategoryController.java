package com.exe101.service_shop.controller;

import com.exe101.service_shop.dto.ServiceCategoryDTO;
import com.exe101.service_shop.entity.ServiceType;
import com.exe101.service_shop.service.ServiceCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-categories")
@RequiredArgsConstructor
public class ServiceCategoryController {

    private final ServiceCategoryService serviceCategoryService;

    @GetMapping
    public ResponseEntity<List<ServiceCategoryDTO>> getAllByShop(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam(required = false) ServiceType serviceType,
            @RequestParam(defaultValue = "true") Boolean active
    ) {
        return ResponseEntity.ok(serviceCategoryService.getAllByShop(shopId, serviceType, active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceCategoryDTO> getById(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(serviceCategoryService.getById(shopId, id));
    }

    @PostMapping
    public ResponseEntity<ServiceCategoryDTO> create(
            @RequestHeader("X-Shop-Id") Long shopId,
            @Valid @RequestBody ServiceCategoryDTO dto
    ) {
        return ResponseEntity.ok(serviceCategoryService.create(shopId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceCategoryDTO> update(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @Valid @RequestBody ServiceCategoryDTO dto
    ) {
        return ResponseEntity.ok(serviceCategoryService.update(shopId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id
    ) {
        serviceCategoryService.delete(shopId, id);
        return ResponseEntity.noContent().build();
    }
}
