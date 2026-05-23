package com.exe101.vaccine.repository;

import com.exe101.vaccine.entity.PetVaccination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IPetVaccinationRepository extends JpaRepository<PetVaccination, Long> {
    List<PetVaccination> findByPetIdOrderByVaccinatedAtDesc(Long petId);

    void deleteByBookingId(Long bookingId);
}
