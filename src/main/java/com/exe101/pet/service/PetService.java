package com.exe101.pet.service;

import com.exe101.common.IService;
import com.exe101.pet.dto.PetDTO;
import com.exe101.pet.entity.Pet;
import com.exe101.pet.exception.PetNotFound;
import com.exe101.pet.mapper.PetMapper;
import com.exe101.pet.repository.IPetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetService implements IService<Pet, PetDTO, Long> {

    private final IPetRepository petRepository;

    @Override
    public List<PetDTO> getAll() {
        return petRepository.findAll().stream().map(PetMapper::toDTO).toList();
    }

    public List<PetDTO> getAllByUserId(Long userId) {
        return petRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(PetMapper::toDTO)
                .toList();
    }

    @Override
    public PetDTO getById(Long id) {
        return petRepository.findById(id)
                .map(PetMapper::toDTO)
                .orElseThrow(() -> new PetNotFound("PetNotFound", "Không tìm thấy thú cưng"));
    }

    public PetDTO getByIdForUser(Long id, Long userId) {
        return findByIdAndUserId(id, userId);
    }

    @Override
    public PetDTO create(PetDTO dto) {
        return PetMapper.toDTO(petRepository.save(PetMapper.toEntity(dto)));
    }

    public PetDTO createForUser(PetDTO dto, Long userId) {
        dto.setUserId(userId);
        return create(dto);
    }

    @Override
    public PetDTO update(Long id, PetDTO dto) {
        Pet entity = petRepository.findById(id)
                .orElseThrow(() -> new PetNotFound("PetNotFound", "Không tìm thấy thú cưng"));
        PetMapper.updateEntity(entity, dto);
        return PetMapper.toDTO(petRepository.save(entity));
    }

    public PetDTO updateForUser(Long id, PetDTO dto, Long userId) {
        findByIdAndUserId(id, userId);
        dto.setUserId(userId);
        return update(id, dto);
    }

    @Override
    public void delete(Long id) {
        if (!petRepository.existsById(id)) {
            throw new PetNotFound("PetNotFound", "Không tìm thấy thú cưng");
        }
        petRepository.deleteById(id);
    }

    public void deleteForUser(Long id, Long userId) {
        findByIdAndUserId(id, userId);
        delete(id);
    }

    private PetDTO findByIdAndUserId(Long id, Long userId) {
        return petRepository.findByIdAndUserId(id, userId)
                .map(PetMapper::toDTO)
                .orElseThrow(() -> new PetNotFound("PetNotFound", "Không tìm thấy thú cưng"));
    }
}
