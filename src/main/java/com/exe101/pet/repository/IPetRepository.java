package com.exe101.pet.repository;

import com.exe101.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IPetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findByShopIdOrderByIdDesc(Long shopId);
}
