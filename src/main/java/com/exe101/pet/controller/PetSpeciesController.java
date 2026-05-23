package com.exe101.pet.controller;

import com.exe101.pet.dto.PetSpeciesDTO;
import com.exe101.pet.service.PetSpeciesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pets/species")
@RequiredArgsConstructor
public class PetSpeciesController {

    private final PetSpeciesService petSpeciesService;

    @GetMapping
    public ResponseEntity<List<PetSpeciesDTO>> getAllSpecies() {
        return ResponseEntity.ok(petSpeciesService.getAll());
    }
}
