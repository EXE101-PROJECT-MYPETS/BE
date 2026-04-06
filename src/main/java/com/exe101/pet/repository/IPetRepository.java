package com.exe101.pet.repository;

import com.exe101.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPetRepository extends JpaRepository<Pet, Long> {
}
