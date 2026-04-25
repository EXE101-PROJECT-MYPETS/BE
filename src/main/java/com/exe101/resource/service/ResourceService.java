package com.exe101.resource.service;

import com.exe101.common.IService;
import com.exe101.resource.dto.ShopResourceDTO;
import com.exe101.resource.entity.ShopResource;
import com.exe101.resource.exception.ResourceNotFound;
import com.exe101.resource.mapper.ShopResourceMapper;
import com.exe101.resource.repository.IShopResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceService implements IService<ShopResource, ShopResourceDTO, Long> {

    private final IShopResourceRepository shopResourceRepository;

    @Override
    public List<ShopResourceDTO> getAll() {
        return shopResourceRepository.findAll().stream().map(ShopResourceMapper::toDTO).toList();
    }

    public List<ShopResourceDTO> getAllByShopId(Long shopId) {
        return shopResourceRepository.findByShopIdOrderByIdDesc(shopId).stream()
                .map(ShopResourceMapper::toDTO)
                .toList();
    }

    @Override
    public ShopResourceDTO getById(Long id) {
        return shopResourceRepository.findById(id)
                .map(ShopResourceMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFound("ResourceNotFound", "Resource not found"));
    }

    @Override
    public ShopResourceDTO create(ShopResourceDTO dto) {
        return ShopResourceMapper.toDTO(shopResourceRepository.save(ShopResourceMapper.toEntity(dto)));
    }

    @Override
    public ShopResourceDTO update(Long id, ShopResourceDTO dto) {
        ShopResource entity = shopResourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("ResourceNotFound", "Resource not found"));
        ShopResourceMapper.updateEntity(entity, dto);
        return ShopResourceMapper.toDTO(shopResourceRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!shopResourceRepository.existsById(id)) {
            throw new ResourceNotFound("ResourceNotFound", "Resource not found");
        }
        shopResourceRepository.deleteById(id);
    }
}
