package com.exe101.service_shop.service;

import com.exe101.common.IService;
import com.exe101.service_shop.dto.ServiceDTO;
import com.exe101.service_shop.exception.ServiceNotFound;
import com.exe101.service_shop.mapper.ServiceMapper;
import com.exe101.service_shop.repository.IServiceRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceService implements IService<com.exe101.service_shop.entity.Service, ServiceDTO, Long> {

    private final IServiceRepository serviceRepository;

    @Override
    public List<ServiceDTO> getAll() {
        return serviceRepository.findAll().stream().map(ServiceMapper::toDTO).toList();
    }

    @Override
    public ServiceDTO getById(Long id) {
        return serviceRepository.findById(id)
                .map(ServiceMapper::toDTO)
                .orElseThrow(() -> new ServiceNotFound("ServiceNotFound", "Service not found"));
    }

    @Override
    public ServiceDTO create(ServiceDTO dto) {
        return ServiceMapper.toDTO(serviceRepository.save(ServiceMapper.toEntity(dto)));
    }

    @Override
    public ServiceDTO update(Long id, ServiceDTO dto) {
        com.exe101.service_shop.entity.Service entity = serviceRepository.findById(id)
                .orElseThrow(() -> new ServiceNotFound("ServiceNotFound", "Service not found"));
        entity.setShopId(dto.getShopId());
        entity.setName(dto.getName());
        entity.setDurationMin(dto.getDurationMin());
        entity.setBasePrice(dto.getBasePrice());
        entity.setActive(dto.getActive() != null ? dto.getActive() : Boolean.TRUE);
        return ServiceMapper.toDTO(serviceRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!serviceRepository.existsById(id)) {
            throw new ServiceNotFound("ServiceNotFound", "Service not found");
        }
        serviceRepository.deleteById(id);
    }
}
