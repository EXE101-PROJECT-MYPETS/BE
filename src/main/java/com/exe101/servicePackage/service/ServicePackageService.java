package com.exe101.servicePackage.service;

import com.exe101.common.IService;
import com.exe101.servicePackage.dto.ServicePackageDTO;
import com.exe101.servicePackage.entity.ServicePackage;
import com.exe101.servicePackage.exception.ServicePackageNotFound;
import com.exe101.servicePackage.mapper.ServicePackageMapper;
import com.exe101.servicePackage.repository.IServicePackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicePackageService implements IService<ServicePackage, ServicePackageDTO, Long> {

    private final IServicePackageRepository servicePackageRepository;

    @Override
    public List<ServicePackageDTO> getAll() {
        return servicePackageRepository.findAll().stream().map(ServicePackageMapper::toDTO).toList();
    }

    public List<ServicePackageDTO> getAllByShopId(Long shopId) {
        return servicePackageRepository.findByShopIdOrderByIdDesc(shopId).stream()
                .map(ServicePackageMapper::toDTO)
                .toList();
    }

    @Override
    public ServicePackageDTO getById(Long id) {
        return servicePackageRepository.findById(id)
                .map(ServicePackageMapper::toDTO)
                .orElseThrow(() -> new ServicePackageNotFound("ServicePackageNotFound", "Package not found"));
    }

    @Override
    public ServicePackageDTO create(ServicePackageDTO dto) {
        return ServicePackageMapper.toDTO(servicePackageRepository.save(ServicePackageMapper.toEntity(dto)));
    }

    @Override
    public ServicePackageDTO update(Long id, ServicePackageDTO dto) {
        ServicePackage entity = servicePackageRepository.findById(id)
                .orElseThrow(() -> new ServicePackageNotFound("ServicePackageNotFound", "Package not found"));
        ServicePackageMapper.updateEntity(entity, dto);
        return ServicePackageMapper.toDTO(servicePackageRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!servicePackageRepository.existsById(id)) {
            throw new ServicePackageNotFound("ServicePackageNotFound", "Package not found");
        }
        servicePackageRepository.deleteById(id);
    }
}
