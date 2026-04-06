package com.exe101.payment.controller;

import com.exe101.payment.dto.PaymentIntentDTO;
import com.exe101.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<List<PaymentIntentDTO>> getAll() {
        return ResponseEntity.ok(paymentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentIntentDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PaymentIntentDTO> create(@Valid @RequestBody PaymentIntentDTO dto) {
        return ResponseEntity.ok(paymentService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentIntentDTO> update(@PathVariable Long id, @Valid @RequestBody PaymentIntentDTO dto) {
        return ResponseEntity.ok(paymentService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
