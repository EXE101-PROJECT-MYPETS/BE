package com.exe101.pet.controller;

import com.exe101.pet.dto.PetBreedDTO;
import com.exe101.pet.service.PetBreedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pets/breeds")
@RequiredArgsConstructor
public class PetBreedController {

    private final PetBreedService petBreedService;

    @GetMapping
    public ResponseEntity<List<PetBreedDTO>> getAllBreeds() {
        return ResponseEntity.ok(petBreedService.getAll());
    }
}
