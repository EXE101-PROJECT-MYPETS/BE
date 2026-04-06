package com.exe101.vaccine.service;

import com.exe101.common.IService;
import com.exe101.vaccine.dto.VaccineDTO;
import com.exe101.vaccine.entity.Vaccine;
import com.exe101.vaccine.exception.VaccineNotFound;
import com.exe101.vaccine.mapper.VaccineMapper;
import com.exe101.vaccine.repository.IVaccineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VaccineService implements IService<Vaccine, VaccineDTO, Long> {

    private final IVaccineRepository vaccineRepository;

    @Override
    public List<VaccineDTO> getAll() {
        return vaccineRepository.findAll().stream().map(VaccineMapper::toDTO).toList();
    }

    @Override
    public VaccineDTO getById(Long id) {
        return vaccineRepository.findById(id)
                .map(VaccineMapper::toDTO)
                .orElseThrow(() -> new VaccineNotFound("VaccineNotFound", "Vaccine not found"));
    }

    @Override
    public VaccineDTO create(VaccineDTO dto) {
        return VaccineMapper.toDTO(vaccineRepository.save(VaccineMapper.toEntity(dto)));
    }

    @Override
    public VaccineDTO update(Long id, VaccineDTO dto) {
        Vaccine entity = vaccineRepository.findById(id)
                .orElseThrow(() -> new VaccineNotFound("VaccineNotFound", "Vaccine not found"));
        VaccineMapper.updateEntity(entity, dto);
        return VaccineMapper.toDTO(vaccineRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!vaccineRepository.existsById(id)) {
            throw new VaccineNotFound("VaccineNotFound", "Vaccine not found");
        }
        vaccineRepository.deleteById(id);
    }
}
