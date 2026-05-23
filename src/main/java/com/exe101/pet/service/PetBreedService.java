package com.exe101.pet.service;

import com.exe101.pet.dto.PetBreedDTO;
import com.exe101.pet.repository.IPetBreedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetBreedService {
    private final IPetBreedRepository petBreedRepository;

    public List<PetBreedDTO> getAll() {
        return petBreedRepository.findAll().stream()
                .map(entity -> new PetBreedDTO(entity.getId(), entity.getSpeciesId(), entity.getName()))
                .toList();
    }
}
