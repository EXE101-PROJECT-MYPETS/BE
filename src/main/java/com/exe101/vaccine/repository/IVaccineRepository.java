package com.exe101.vaccine.repository;

import com.exe101.vaccine.entity.Vaccine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IVaccineRepository extends JpaRepository<Vaccine, Long> {
}
