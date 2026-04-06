package com.exe101.pet.repository;

import com.exe101.pet.entity.PetHealthProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPetHealthProfileRepository extends JpaRepository<PetHealthProfile, Long> {
}
