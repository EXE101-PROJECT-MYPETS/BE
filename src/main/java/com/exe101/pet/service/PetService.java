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

    public List<PetDTO> getAllByShopId(Long shopId) {
        return petRepository.findByShopIdOrderByIdDesc(shopId).stream()
                .map(PetMapper::toDTO)
                .toList();
    }

    @Override
    public PetDTO getById(Long id) {
        return petRepository.findById(id)
                .map(PetMapper::toDTO)
                .orElseThrow(() -> new PetNotFound("PetNotFound", "Không tìm thấy thú cưng"));
    }

    @Override
    public PetDTO create(PetDTO dto) {
        return PetMapper.toDTO(petRepository.save(PetMapper.toEntity(dto)));
    }

    @Override
    public PetDTO update(Long id, PetDTO dto) {
        Pet entity = petRepository.findById(id)
                .orElseThrow(() -> new PetNotFound("PetNotFound", "Không tìm thấy thú cưng"));
        PetMapper.updateEntity(entity, dto);
        return PetMapper.toDTO(petRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!petRepository.existsById(id)) {
            throw new PetNotFound("PetNotFound", "Không tìm thấy thú cưng");
        }
        petRepository.deleteById(id);
    }
}
