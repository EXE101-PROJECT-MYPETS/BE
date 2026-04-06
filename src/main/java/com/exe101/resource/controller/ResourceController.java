package com.exe101.resource.controller;

import com.exe101.resource.dto.ShopResourceDTO;
import com.exe101.resource.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<List<ShopResourceDTO>> getAll() {
        return ResponseEntity.ok(resourceService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShopResourceDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ShopResourceDTO> create(@Valid @RequestBody ShopResourceDTO dto) {
        return ResponseEntity.ok(resourceService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShopResourceDTO> update(@PathVariable Long id, @Valid @RequestBody ShopResourceDTO dto) {
        return ResponseEntity.ok(resourceService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
