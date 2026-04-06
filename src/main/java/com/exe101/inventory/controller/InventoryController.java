package com.exe101.inventory.controller;

import com.exe101.inventory.dto.InventoryDTO;
import com.exe101.inventory.entity.InventoryId;
import com.exe101.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<InventoryDTO>> getAll() {
        return ResponseEntity.ok(inventoryService.getAll());
    }

    @GetMapping("/{shopId}/{productId}")
    public ResponseEntity<InventoryDTO> getById(@PathVariable Long shopId, @PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getById(new InventoryId(shopId, productId)));
    }

    @PostMapping
    public ResponseEntity<InventoryDTO> create(@Valid @RequestBody InventoryDTO dto) {
        return ResponseEntity.ok(inventoryService.create(dto));
    }

    @PutMapping("/{shopId}/{productId}")
    public ResponseEntity<InventoryDTO> update(
            @PathVariable Long shopId,
            @PathVariable Long productId,
            @Valid @RequestBody InventoryDTO dto
    ) {
        return ResponseEntity.ok(inventoryService.update(new InventoryId(shopId, productId), dto));
    }

    @DeleteMapping("/{shopId}/{productId}")
    public ResponseEntity<Void> delete(@PathVariable Long shopId, @PathVariable Long productId) {
        inventoryService.delete(new InventoryId(shopId, productId));
        return ResponseEntity.noContent().build();
    }
}
