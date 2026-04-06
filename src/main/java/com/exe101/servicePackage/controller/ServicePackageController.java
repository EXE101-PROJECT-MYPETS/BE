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
    public ResponseEntity<List<ServicePackageDTO>> getAll() {
        return ResponseEntity.ok(servicePackageService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicePackageDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(servicePackageService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ServicePackageDTO> create(@Valid @RequestBody ServicePackageDTO dto) {
        return ResponseEntity.ok(servicePackageService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicePackageDTO> update(@PathVariable Long id, @Valid @RequestBody ServicePackageDTO dto) {
        return ResponseEntity.ok(servicePackageService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        servicePackageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
