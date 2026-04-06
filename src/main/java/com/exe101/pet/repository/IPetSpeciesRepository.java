package com.exe101.pet.repository;

import com.exe101.pet.entity.PetSpecies;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPetSpeciesRepository extends JpaRepository<PetSpecies, Long> {
}
