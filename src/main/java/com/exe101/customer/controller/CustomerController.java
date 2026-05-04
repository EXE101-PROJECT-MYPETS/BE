package com.exe101.customer.controller;

import com.exe101.customer.dto.CustomerDTO;
import com.exe101.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAll(@RequestHeader("X-Shop-Id") Long shopId) {
        return ResponseEntity.ok(customerService.getAllByShopId(shopId));
    }

    @GetMapping("/verify-phone")
    public ResponseEntity<CustomerDTO> verifyByPhone(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam String phone
    ) {
        return ResponseEntity.ok(customerService.verifyByPhone(shopId, phone));
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<CustomerDTO>> suggestByPhone(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam String phone,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(customerService.suggestByPhone(shopId, phone, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getById(id));
    }

    @PostMapping
    public ResponseEntity<CustomerDTO> create(
            @RequestHeader("X-Shop-Id") Long shopId,
            @Valid @RequestBody CustomerDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(customerService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> update(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @Valid @RequestBody CustomerDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(customerService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
