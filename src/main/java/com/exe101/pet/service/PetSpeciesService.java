package com.exe101.pet.service;

import com.exe101.pet.dto.PetSpeciesDTO;
import com.exe101.pet.repository.IPetSpeciesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetSpeciesService {
    private final IPetSpeciesRepository petSpeciesRepository;

    public List<PetSpeciesDTO> getAll() {
        return petSpeciesRepository.findAll().stream()
                .map(entity -> new PetSpeciesDTO(entity.getId(), entity.getName()))
                .toList();
    }
}
