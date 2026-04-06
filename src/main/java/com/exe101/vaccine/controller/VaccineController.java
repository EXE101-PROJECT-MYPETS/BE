package com.exe101.vaccine.controller;

import com.exe101.vaccine.dto.VaccineDTO;
import com.exe101.vaccine.service.VaccineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vaccines")
@RequiredArgsConstructor
public class VaccineController {

    private final VaccineService vaccineService;

    @GetMapping
    public ResponseEntity<List<VaccineDTO>> getAll() {
        return ResponseEntity.ok(vaccineService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VaccineDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(vaccineService.getById(id));
    }

    @PostMapping
    public ResponseEntity<VaccineDTO> create(@Valid @RequestBody VaccineDTO dto) {
        return ResponseEntity.ok(vaccineService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VaccineDTO> update(@PathVariable Long id, @Valid @RequestBody VaccineDTO dto) {
        return ResponseEntity.ok(vaccineService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vaccineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
