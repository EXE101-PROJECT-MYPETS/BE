package com.exe101.inventory.service;

import com.exe101.common.IService;
import com.exe101.inventory.dto.InventoryDTO;
import com.exe101.inventory.entity.Inventory;
import com.exe101.inventory.entity.InventoryId;
import com.exe101.inventory.exception.InventoryNotFound;
import com.exe101.inventory.mapper.InventoryMapper;
import com.exe101.inventory.repository.IInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService implements IService<Inventory, InventoryDTO, InventoryId> {

    private final IInventoryRepository inventoryRepository;

    @Override
    public List<InventoryDTO> getAll() {
        return inventoryRepository.findAll().stream().map(InventoryMapper::toDTO).toList();
    }

    @Override
    public InventoryDTO getById(InventoryId id) {
        return inventoryRepository.findById(id)
                .map(InventoryMapper::toDTO)
                .orElseThrow(() -> new InventoryNotFound("InventoryNotFound", "Inventory not found"));
    }

    @Override
    public InventoryDTO create(InventoryDTO dto) {
        return InventoryMapper.toDTO(inventoryRepository.save(InventoryMapper.toEntity(dto)));
    }

    @Override
    public InventoryDTO update(InventoryId id, InventoryDTO dto) {
        Inventory entity = inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFound("InventoryNotFound", "Inventory not found"));
        InventoryMapper.updateEntity(entity, dto);
        return InventoryMapper.toDTO(inventoryRepository.save(entity));
    }

    @Override
    public void delete(InventoryId id) {
        if (!inventoryRepository.existsById(id)) {
            throw new InventoryNotFound("InventoryNotFound", "Inventory not found");
        }
        inventoryRepository.deleteById(id);
    }
}
