package com.exe101.veterinary.service;

import com.exe101.pet.exception.PetNotFound;
import com.exe101.pet.repository.IPetRepository;
import com.exe101.vaccine.dto.PetVaccinationDTO;
import com.exe101.vaccine.mapper.PetVaccinationMapper;
import com.exe101.vaccine.repository.IPetVaccinationRepository;
import com.exe101.veterinary.dto.PetMedicalRecordDTO;
import com.exe101.veterinary.mapper.PetMedicalRecordMapper;
import com.exe101.veterinary.repository.IPetMedicalRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetHistoryService {

    private final IPetRepository petRepository;
    private final IPetVaccinationRepository petVaccinationRepository;
    private final IPetMedicalRecordRepository petMedicalRecordRepository;

    @Transactional(readOnly = true)
    public List<PetMedicalRecordDTO> getMedicalRecordsForUser(Long petId, Long userId) {
        assertPetOwnership(petId, userId);
        return petMedicalRecordRepository.findByPetIdOrderByPerformedAtDescIdDesc(petId).stream()
                .map(PetMedicalRecordMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PetVaccinationDTO> getVaccinationsForUser(Long petId, Long userId) {
        assertPetOwnership(petId, userId);
        return petVaccinationRepository.findByPetIdOrderByVaccinatedAtDesc(petId).stream()
                .map(PetVaccinationMapper::toDTO)
                .toList();
    }

    private void assertPetOwnership(Long petId, Long userId) {
        if (petRepository.findByIdAndUserId(petId, userId).isEmpty()) {
            throw new PetNotFound("PetNotFound", "Không tìm thấy thú cưng");
        }
    }
}

