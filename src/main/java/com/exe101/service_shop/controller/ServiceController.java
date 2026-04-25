package com.exe101.service_shop.controller;

import com.exe101.common.ScrollResponse;
import com.exe101.service_shop.dto.ServiceDTO;
import com.exe101.service_shop.service.ServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    @GetMapping("/services")
    public ResponseEntity<ScrollResponse<ServiceDTO>> getAll(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(serviceService.getAllForScroll(
                shopId,
                search,
                categoryId,
                active,
                cursor,
                size
        ));
    }

    @GetMapping("/services/{id}")
    public ResponseEntity<ServiceDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceService.getById(id));
    }

    @PostMapping("/services")
    public ResponseEntity<ServiceDTO> create(
            @RequestHeader("X-Shop-Id") Long shopId,
            @Valid @RequestBody ServiceDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(serviceService.create(dto));
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<ServiceDTO> update(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @Valid @RequestBody ServiceDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(serviceService.update(id, dto));
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
