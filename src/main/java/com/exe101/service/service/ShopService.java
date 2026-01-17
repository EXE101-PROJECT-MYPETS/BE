package com.exe101.service.service;

import com.exe101.common.IService;
import com.exe101.shop.dto.ShopDTO;
import com.exe101.shop.entity.Shop;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShopService implements IService<Shop, ShopDTO, Long> {
    @Override
    public List<ShopDTO> getAll() {
        return List.of();
    }

    @Override
    public ShopDTO getById(Long aLong) {
        return null;
    }

    @Override
    public ShopDTO create(ShopDTO dto) {
        return null;
    }

    @Override
    public ShopDTO update(Long aLong, ShopDTO dto) {
        return null;
    }

    @Override
    public void delete(Long aLong) {

    }
}
