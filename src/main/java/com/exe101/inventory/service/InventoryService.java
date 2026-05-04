package com.exe101.inventory.service;

import com.exe101.common.IService;
import com.exe101.common.ScrollResponse;
import com.exe101.inventory.dto.InventoryDTO;
import com.exe101.inventory.entity.Inventory;
import com.exe101.inventory.entity.InventoryId;
import com.exe101.inventory.exception.InventoryNotFound;
import com.exe101.inventory.mapper.InventoryMapper;
import com.exe101.inventory.repository.IInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService implements IService<Inventory, InventoryDTO, InventoryId> {

    private static final int MAX_SCROLL_SIZE = 50;

    private final IInventoryRepository inventoryRepository;

    @Override
    public List<InventoryDTO> getAll() {
        return inventoryRepository.findAll().stream().map(InventoryMapper::toDTO).toList();
    }

    public List<InventoryDTO> getAllByShopId(Long shopId) {
        return inventoryRepository.findByShopId(shopId).stream()
                .map(InventoryMapper::toDTO)
                .toList();
    }

    public ScrollResponse<InventoryDTO> getAllForScroll(Long shopId, Long cursor, int size) {
        int normalizedSize = Math.min(Math.max(size, 1), MAX_SCROLL_SIZE);
        Long normalizedCursor = cursor != null && cursor > 0 ? cursor : null;

        List<Inventory> inventories = inventoryRepository.findForScroll(
                shopId,
                normalizedCursor,
                PageRequest.of(0, normalizedSize + 1)
        );

        boolean hasNext = inventories.size() > normalizedSize;
        List<InventoryDTO> content = inventories.stream()
                .limit(normalizedSize)
                .map(InventoryMapper::toDTO)
                .toList();
        Long nextCursor = hasNext && !content.isEmpty()
                ? content.get(content.size() - 1).getProductId()
                : null;

        return ScrollResponse.of(content, normalizedSize, nextCursor, hasNext);
    }

    @Override
    public InventoryDTO getById(InventoryId id) {
        return inventoryRepository.findById(id)
                .map(InventoryMapper::toDTO)
                .orElseThrow(() -> new InventoryNotFound("InventoryNotFound", "Không tìm thấy tồn kho"));
    }

    @Override
    public InventoryDTO create(InventoryDTO dto) {
        return InventoryMapper.toDTO(inventoryRepository.save(InventoryMapper.toEntity(dto)));
    }

    @Override
    public InventoryDTO update(InventoryId id, InventoryDTO dto) {
        Inventory entity = inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFound("InventoryNotFound", "Không tìm thấy tồn kho"));
        InventoryMapper.updateEntity(entity, dto);
        return InventoryMapper.toDTO(inventoryRepository.save(entity));
    }

    @Override
    public void delete(InventoryId id) {
        if (!inventoryRepository.existsById(id)) {
            throw new InventoryNotFound("InventoryNotFound", "Không tìm thấy tồn kho");
        }
        inventoryRepository.deleteById(id);
    }
}
