package com.exe101.vaccine.repository;

import com.exe101.vaccine.entity.PetVaccination;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPetVaccinationRepository extends JpaRepository<PetVaccination, Long> {
}
