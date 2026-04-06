package com.exe101.pet.repository;

import com.exe101.pet.entity.PetBreed;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPetBreedRepository extends JpaRepository<PetBreed, Long> {
}
