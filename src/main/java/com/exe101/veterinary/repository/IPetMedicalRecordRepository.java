package com.exe101.veterinary.repository;

import com.exe101.veterinary.entity.PetMedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IPetMedicalRecordRepository extends JpaRepository<PetMedicalRecord, Long> {
    List<PetMedicalRecord> findByPetIdOrderByPerformedAtDescIdDesc(Long petId);

    void deleteByBookingId(Long bookingId);
}
