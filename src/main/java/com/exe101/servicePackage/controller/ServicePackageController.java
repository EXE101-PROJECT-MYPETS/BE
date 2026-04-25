package com.exe101.servicePackage.controller;

import com.exe101.servicePackage.dto.ServicePackageDTO;
import com.exe101.servicePackage.service.ServicePackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class ServicePackageController {

    private final ServicePackageService servicePackageService;

    @GetMapping
    public ResponseEntity<List<ServicePackageDTO>> getAll(@RequestHeader("X-Shop-Id") Long shopId) {
        return ResponseEntity.ok(servicePackageService.getAllByShopId(shopId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicePackageDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(servicePackageService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ServicePackageDTO> create(
            @RequestHeader("X-Shop-Id") Long shopId,
            @Valid @RequestBody ServicePackageDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(servicePackageService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicePackageDTO> update(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @Valid @RequestBody ServicePackageDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(servicePackageService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        servicePackageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
