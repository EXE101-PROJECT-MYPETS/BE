package com.exe101.inventory.controller;

import com.exe101.common.ScrollResponse;
import com.exe101.inventory.dto.InventoryDTO;
import com.exe101.inventory.entity.InventoryId;
import com.exe101.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<ScrollResponse<InventoryDTO>> getAll(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(inventoryService.getAllForScroll(shopId, cursor, size));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryDTO> getById(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(inventoryService.getById(new InventoryId(shopId, productId)));
    }

    @PostMapping
    public ResponseEntity<InventoryDTO> create(
            @RequestHeader("X-Shop-Id") Long shopId,
            @Valid @RequestBody InventoryDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(inventoryService.create(dto));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<InventoryDTO> update(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long productId,
            @Valid @RequestBody InventoryDTO dto
    ) {
        dto.setShopId(shopId);
        dto.setProductId(productId);
        return ResponseEntity.ok(inventoryService.update(new InventoryId(shopId, productId), dto));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long productId
    ) {
        inventoryService.delete(new InventoryId(shopId, productId));
        return ResponseEntity.noContent().build();
    }
}
