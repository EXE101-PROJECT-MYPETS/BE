package com.exe101.pet.controller;

import com.exe101.pet.dto.PetDTO;
import com.exe101.pet.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @GetMapping
    public ResponseEntity<List<PetDTO>> getAll() {
        return ResponseEntity.ok(petService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PetDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(petService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PetDTO> create(@Valid @RequestBody PetDTO dto) {
        return ResponseEntity.ok(petService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PetDTO> update(@PathVariable Long id, @Valid @RequestBody PetDTO dto) {
        return ResponseEntity.ok(petService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        petService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
