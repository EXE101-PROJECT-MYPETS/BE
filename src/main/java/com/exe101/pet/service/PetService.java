package com.exe101.pet.service;

import com.exe101.common.IService;
import com.exe101.file.FileUploadUtil;
import com.exe101.pet.dto.PetDTO;
import com.exe101.pet.entity.Pet;
import com.exe101.pet.entity.PetShopLink;
import com.exe101.pet.exception.PetNotFound;
import com.exe101.pet.mapper.PetMapper;
import com.exe101.pet.repository.IPetRepository;
import com.exe101.pet.repository.IPetShopLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetService implements IService<Pet, PetDTO, Long> {

    private final IPetRepository petRepository;
    private final IPetShopLinkRepository petShopLinkRepository;
    private final FileUploadUtil fileUploadUtil;

    @Override
    public List<PetDTO> getAll() {
        return petRepository.findAll().stream()
                .map(PetMapper::toDTO)
                .map(this::normalizeAvatarUrl)
                .toList();
    }

    public List<PetDTO> getAllByUserId(Long userId) {
        return petRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(PetMapper::toDTO)
                .map(this::normalizeAvatarUrl)
                .toList();
    }

    @Override
    public PetDTO getById(Long id) {
        return petRepository.findById(id)
                .map(PetMapper::toDTO)
                .map(this::normalizeAvatarUrl)
                .orElseThrow(() -> new PetNotFound("PetNotFound", "Không tìm thấy thú cưng"));
    }

    public PetDTO getByIdForUser(Long id, Long userId) {
        return petRepository.findByIdAndUserId(id, userId)
                .map(PetMapper::toDTO)
                .map(this::normalizeAvatarUrl)
                .orElseThrow(() -> new PetNotFound("PetNotFound", "Không tìm thấy thú cưng"));
    }

    @Override
    public PetDTO create(PetDTO dto) {
        return PetMapper.toDTO(petRepository.save(PetMapper.toEntity(dto)));
    }

    public PetDTO createForUser(Long userId, Long shopId, PetDTO dto) {
        dto.setUserId(userId);
        Pet saved = petRepository.save(PetMapper.toEntity(dto));
        saveAvatarIfProvided(userId, saved, dto);
        linkPetToShopIfProvided(saved.getId(), shopId);
        return normalizeAvatarUrl(PetMapper.toDTO(saved));
    }

    @Override
    public PetDTO update(Long id, PetDTO dto) {
        Pet entity = petRepository.findById(id)
                .orElseThrow(() -> new PetNotFound("PetNotFound", "Không tìm thấy thú cưng"));
        PetMapper.updateEntity(entity, dto);
        return PetMapper.toDTO(petRepository.save(entity));
    }

    public PetDTO updateForUser(Long id, Long userId, Long shopId, PetDTO dto) {
        Pet entity = petRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new PetNotFound("PetNotFound", "Không tìm thấy thú cưng"));

        dto.setUserId(userId);
        PetMapper.updateEntity(entity, dto);
        Pet saved = petRepository.save(entity);
        saveAvatarIfProvided(userId, saved, dto);
        linkPetToShopIfProvided(saved.getId(), shopId);
        return normalizeAvatarUrl(PetMapper.toDTO(saved));
    }

    @Override
    public void delete(Long id) {
        if (!petRepository.existsById(id)) {
            throw new PetNotFound("PetNotFound", "Không tìm thấy thú cưng");
        }
        petRepository.deleteById(id);
    }

    public void deleteForUser(Long id, Long userId) {
        if (!petRepository.existsByIdAndUserId(id, userId)) {
            throw new PetNotFound("PetNotFound", "Không tìm thấy thú cưng");
        }
        petRepository.deleteById(id);
    }

    public void linkPetToShop(Long userId, Long petId, Long shopId) {
        petRepository.findByIdAndUserId(petId, userId)
                .orElseThrow(() -> new PetNotFound("PetNotFound", "Không tìm thấy thú cưng"));
        linkPetToShopIfProvided(petId, shopId);
    }

    private void linkPetToShopIfProvided(Long petId, Long shopId) {
        if (shopId == null) {
            return;
        }
        if (!petShopLinkRepository.existsByPetIdAndShopId(petId, shopId)) {
            PetShopLink link = new PetShopLink();
            link.setPetId(petId);
            link.setShopId(shopId);
            petShopLinkRepository.save(link);
        }
    }

    private void saveAvatarIfProvided(Long userId, Pet pet, PetDTO dto) {
        if (dto.getAvatarUrlPreview() == null || dto.getAvatarUrlPreview().isEmpty()) {
            return;
        }
        String avatarUrl = fileUploadUtil.uploadPetAvatar(userId, pet.getId(), dto.getAvatarUrlPreview());
        pet.setAvatarUrl(avatarUrl);
        petRepository.save(pet);
    }

    private PetDTO normalizeAvatarUrl(PetDTO dto) {
        if (dto == null) {
            return null;
        }
        dto.setAvatarUrl(fileUploadUtil.normalizePetAvatarPath(dto.getAvatarUrl()));
        return dto;
    }
}
