package com.exe101.ai.service;

import com.exe101.ai.dto.PetContext;
import com.exe101.ai.exception.AiAccessDenied;
import com.exe101.ai.exception.AiNotFound;
import com.exe101.pet.entity.Pet;
import com.exe101.pet.entity.PetHealthProfile;
import com.exe101.pet.repository.IPetHealthProfileRepository;
import com.exe101.pet.repository.IPetRepository;
import com.exe101.vaccine.entity.PetVaccination;
import com.exe101.vaccine.repository.IPetVaccinationRepository;
import com.exe101.veterinary.entity.PetMedicalRecord;
import com.exe101.veterinary.repository.IPetMedicalRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AiPetContextService {

    private final IPetRepository petRepository;
    private final IPetHealthProfileRepository petHealthProfileRepository;
    private final IPetVaccinationRepository petVaccinationRepository;
    private final IPetMedicalRecordRepository petMedicalRecordRepository;

    @Transactional(readOnly = true)
    public String buildPetContext(Long petId, Long currentUserId) {
        return buildPetContext(loadPetContext(petId, currentUserId));
    }

    @Transactional(readOnly = true)
    public PetContext loadPetContext(Long petId, Long currentUserId) {
        Pet pet = getAuthorizedPet(petId, currentUserId);
        PetHealthProfile healthProfile = petHealthProfileRepository.findById(petId).orElse(null);
        List<PetVaccination> vaccinations = petVaccinationRepository.findByPetIdOrderByVaccinatedAtDesc(petId);
        List<PetMedicalRecord> medicalRecords = petMedicalRecordRepository.findByPetIdOrderByPerformedAtDescIdDesc(petId);

        return PetContext.builder()
                .petId(pet.getId())
                .name(defaultText(pet.getName()))
                .species(resolveSpeciesCode(pet))
                .speciesName(pet.getSpecies() == null ? "khong ro" : defaultText(pet.getSpecies().getName()))
                .breed(resolveBreedName(pet))
                .age(resolveAge(pet))
                .weight(pet.getWeightKg() == null ? "khong co" : pet.getWeightKg() + " kg")
                .gender(defaultText(pet.getGender()))
                .generalNote(defaultText(pet.getNote()))
                .allergies(healthProfile == null ? "khong co" : defaultText(healthProfile.getAllergies()))
                .conditions(healthProfile == null ? "khong co" : defaultText(healthProfile.getConditions()))
                .healthNotes(healthProfile == null ? "khong co" : defaultText(healthProfile.getNotes()))
                .vaccinationSummary(buildVaccinationSummary(vaccinations))
                .medicalRecordSummary(buildMedicalRecordSummary(medicalRecords))
                .build();
    }

    public String buildPetContext(PetContext petContext) {
        StringBuilder builder = new StringBuilder();
        builder.append("Thong tin thu cung:\n");
        builder.append("- Ten: ").append(defaultText(petContext.getName())).append('\n');
        builder.append("- Loai: ").append(defaultText(petContext.getSpeciesName())).append('\n');
        builder.append("- Giong: ").append(defaultText(petContext.getBreed())).append('\n');
        builder.append("- Tuoi: ").append(defaultText(petContext.getAge())).append('\n');
        builder.append("- Can nang: ").append(defaultText(petContext.getWeight())).append('\n');
        builder.append("- Gioi tinh: ").append(defaultText(petContext.getGender())).append('\n');
        builder.append("- Ghi chu suc khoe: ")
                .append("di ung: ").append(defaultText(petContext.getAllergies()))
                .append("; benh nen: ").append(defaultText(petContext.getConditions()))
                .append("; ghi chu: ").append(defaultText(petContext.getHealthNotes()))
                .append('\n');
        builder.append("- Ghi chu chung: ").append(defaultText(petContext.getGeneralNote())).append('\n');
        builder.append("Lich su tiem phong gan day:\n");
        builder.append(defaultText(petContext.getVaccinationSummary()));
        builder.append("Lich su kham/thu y gan day:\n");
        builder.append(defaultText(petContext.getMedicalRecordSummary()));
        return builder.toString();
    }

    private String buildVaccinationSummary(List<PetVaccination> vaccinations) {
        StringBuilder builder = new StringBuilder();
        if (vaccinations.isEmpty()) {
            builder.append("- Chua co du lieu tiem phong\n");
        } else {
            vaccinations.stream().limit(10).forEach(vaccination -> builder
                    .append("- ")
                    .append(vaccination.getVaccine() == null ? "Khong ro vaccine" : defaultText(vaccination.getVaccine().getName()))
                    .append(", ngay tiem: ").append(vaccination.getVaccinatedAt())
                    .append(", hen nhac lai: ").append(vaccination.getNextDueAt() == null ? "khong co" : vaccination.getNextDueAt())
                    .append('\n'));
        }
        return builder.toString();
    }

    private String buildMedicalRecordSummary(List<PetMedicalRecord> medicalRecords) {
        StringBuilder builder = new StringBuilder();
        if (medicalRecords.isEmpty()) {
            builder.append("- Chua co du lieu kham/thu y\n");
        } else {
            medicalRecords.stream().limit(10).forEach(record -> builder
                    .append("- Ngay: ").append(record.getPerformedAt() == null ? "khong ro" : record.getPerformedAt())
                    .append(", dich vu: ").append(record.getService() == null ? "khong ro" : defaultText(record.getService().getName()))
                    .append(", trieu chung: ").append(defaultText(record.getSymptoms()))
                    .append(", chan doan: ").append(defaultText(record.getDiagnosis()))
                    .append(", dieu tri: ").append(defaultText(record.getTreatment()))
                    .append(", ghi chu: ").append(defaultText(record.getNote()))
                    .append(", hen tai kham: ")
                    .append(record.getFollowUpAt() == null ? "khong co" : record.getFollowUpAt())
                    .append('\n'));
        }
        return builder.toString();
    }

    @Transactional(readOnly = true)
    public Pet getAuthorizedPet(Long petId, Long currentUserId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new AiNotFound("AiPetNotFound", "Không tìm thấy thú cưng"));

        if (pet.getUserId() == null) {
            throw new AiAccessDenied("AiPetAccessDenied", "Không xác định được chủ sở hữu của thú cưng");
        }
        if (!pet.getUserId().equals(currentUserId)) {
            throw new AiAccessDenied("AiPetAccessDenied", "Bạn không có quyền truy cập thú cưng này");
        }
        return pet;
    }

    private String resolveBreedName(Pet pet) {
        if (pet.getBreed() != null && pet.getBreed().getName() != null && !pet.getBreed().getName().isBlank()) {
            return pet.getBreed().getName();
        }
        return defaultText(pet.getBreedText());
    }

    private String resolveAge(Pet pet) {
        if (pet.getDob() == null) {
            return "khong ro";
        }
        Period age = Period.between(pet.getDob(), LocalDate.now());
        if (age.getYears() > 0) {
            return age.getYears() + " nam " + age.getMonths() + " thang";
        }
        return age.getMonths() + " thang";
    }

    private String resolveSpeciesCode(Pet pet) {
        if (pet.getSpecies() == null || pet.getSpecies().getName() == null) {
            return "UNKNOWN";
        }
        String name = pet.getSpecies().getName().toLowerCase(Locale.ROOT);
        if (name.contains("dog") || name.contains("cho") || name.contains("chó")) {
            return "DOG";
        }
        if (name.contains("cat") || name.contains("meo") || name.contains("mèo")) {
            return "CAT";
        }
        return "OTHER";
    }

    private String defaultText(String value) {
        return value == null || value.isBlank() ? "khong co" : value;
    }
}
