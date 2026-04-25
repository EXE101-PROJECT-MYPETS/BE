package com.exe101.invoice.controller;

import com.exe101.invoice.dto.InvoiceDTO;
import com.exe101.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> getAll(@RequestHeader("X-Shop-Id") Long shopId) {
        return ResponseEntity.ok(invoiceService.getAllByShopId(shopId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getById(id));
    }

    @PostMapping
    public ResponseEntity<InvoiceDTO> create(
            @RequestHeader("X-Shop-Id") Long shopId,
            @Valid @RequestBody InvoiceDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(invoiceService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceDTO> update(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @Valid @RequestBody InvoiceDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(invoiceService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
