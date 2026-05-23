package com.exe101.pet.repository;

import com.exe101.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IPetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findByUserIdOrderByIdDesc(Long userId);

    Optional<Pet> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
