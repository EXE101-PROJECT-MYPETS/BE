package com.exe101.ai.service;

import com.exe101.ai.exception.AiAccessDenied;
import com.exe101.ai.exception.AiNotFound;
import com.exe101.pet.entity.Pet;
import com.exe101.pet.entity.PetHealthProfile;
import com.exe101.pet.repository.IPetHealthProfileRepository;
import com.exe101.pet.repository.IPetRepository;
import com.exe101.vaccine.entity.PetVaccination;
import com.exe101.vaccine.repository.IPetVaccinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiPetContextService {

    private final IPetRepository petRepository;
    private final IPetHealthProfileRepository petHealthProfileRepository;
    private final IPetVaccinationRepository petVaccinationRepository;

    @Transactional(readOnly = true)
    public String buildPetContext(Long petId, Long currentUserId) {
        Pet pet = getAuthorizedPet(petId, currentUserId);
        PetHealthProfile healthProfile = petHealthProfileRepository.findById(petId).orElse(null);
        List<PetVaccination> vaccinations = petVaccinationRepository.findByPetIdOrderByVaccinatedAtDesc(petId);

        StringBuilder builder = new StringBuilder();
        builder.append("Thong tin thu cung:\n");
        builder.append("- Pet ID: ").append(pet.getId()).append('\n');
        builder.append("- Ten: ").append(defaultText(pet.getName())).append('\n');
        builder.append("- Gioi tinh: ").append(defaultText(pet.getGender())).append('\n');
        builder.append("- Ngay sinh: ").append(pet.getDob() == null ? "khong ro" : pet.getDob()).append('\n');
        builder.append("- Loai: ").append(pet.getSpecies() == null ? "khong ro" : defaultText(pet.getSpecies().getName())).append('\n');
        builder.append("- Giong: ").append(resolveBreedName(pet)).append('\n');
        builder.append("- Ghi chu chung: ").append(defaultText(pet.getNote())).append('\n');

        if (healthProfile != null) {
            builder.append("Ho so suc khoe:\n");
            builder.append("- Di ung: ").append(defaultText(healthProfile.getAllergies())).append('\n');
            builder.append("- Benh nen: ").append(defaultText(healthProfile.getConditions())).append('\n');
            builder.append("- Ghi chu: ").append(defaultText(healthProfile.getNotes())).append('\n');
        }

        builder.append("Lich su tiem phong gan day:\n");
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

    private String defaultText(String value) {
        return value == null || value.isBlank() ? "khong co" : value;
    }
}
