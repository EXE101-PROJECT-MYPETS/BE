package com.exe101.customerAddress.controller;

import com.exe101.customerAddress.dto.CustomerAddressDTO;
import com.exe101.customerAddress.service.CustomerAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer-addresses")
@RequiredArgsConstructor
public class CustomerAddressController {

    private final CustomerAddressService customerAddressService;

    @GetMapping
    public ResponseEntity<List<CustomerAddressDTO>> getAll(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam Long customerId
    ) {
        return ResponseEntity.ok(customerAddressService.getAllByCustomerId(shopId, customerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerAddressDTO> getById(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(customerAddressService.getById(shopId, id));
    }

    @PostMapping
    public ResponseEntity<CustomerAddressDTO> create(
            @RequestHeader("X-Shop-Id") Long shopId,
            @Valid @RequestBody CustomerAddressDTO dto
    ) {
        return ResponseEntity.ok(customerAddressService.create(shopId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerAddressDTO> update(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @Valid @RequestBody CustomerAddressDTO dto
    ) {
        return ResponseEntity.ok(customerAddressService.update(shopId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id
    ) {
        customerAddressService.delete(shopId, id);
        return ResponseEntity.noContent().build();
    }
}
