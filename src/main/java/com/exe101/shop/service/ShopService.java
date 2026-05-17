package com.exe101.shop.service;

import com.exe101.common.IService;
import com.exe101.file.FileUploadUtil;
import com.exe101.shop.dto.ShopDTO;
import com.exe101.shop.dto.ShopProfileUpdateRequest;
import com.exe101.shop.entity.Shop;
import com.exe101.shop.exception.ShopNotFound;
import com.exe101.shop.mapper.ShopMapper;
import com.exe101.shop.repository.IShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopService implements IService<Shop, ShopDTO, Long> {

    private final IShopRepository shopRepository;
    private final FileUploadUtil fileUploadUtil;

    @Override
    @Transactional(readOnly = true)
    public List<ShopDTO> getAll() {
        return shopRepository.findAllByOrderByIdAsc().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShopDTO getById(Long id) {
        return shopRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ShopNotFound("ShopNotFound", "Khong tim thay shop"));
    }

    @Override
    @Transactional
    public ShopDTO create(ShopDTO dto) {
        Shop entity = ShopMapper.toEntity(dto);
        entity.setImageUrl(normalizeImageStoragePath(dto.getImageUrl()));
        entity.setCoverImageUrl(normalizeImageStoragePath(dto.getCoverImageUrl()));
        return toDTO(shopRepository.save(entity));
    }

    @Override
    @Transactional
    public ShopDTO update(Long id, ShopDTO dto) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ShopNotFound("ShopNotFound", "Khong tim thay shop"));
        ShopMapper.updateEntity(shop, dto);
        shop.setImageUrl(normalizeImageStoragePath(dto.getImageUrl()));
        shop.setCoverImageUrl(normalizeImageStoragePath(dto.getCoverImageUrl()));
        return toDTO(shopRepository.save(shop));
    }

    @Transactional
    public ShopDTO update(Long shopId, ShopProfileUpdateRequest request) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFound("ShopNotFound", "Khong tim thay shop"));

        applyWriteData(shop, request);
        applyImageUpdates(shop, request);

        return toDTO(shopRepository.save(shop));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!shopRepository.existsById(id)) {
            throw new ShopNotFound("ShopNotFound", "Khong tim thay shop");
        }
        shopRepository.deleteById(id);
    }

    private void applyWriteData(Shop shop, ShopProfileUpdateRequest request) {
        shop.setName(request.getName());
        shop.setAddressText(request.getAddressText());
        shop.setPhone(request.getPhone());
        shop.setEmail(request.getEmail());
        shop.setDescription(request.getDescription());
        shop.setOpeningHours(request.getOpeningHours());
        shop.setClosingHours(request.getClosingHours());
        shop.setFacebookUrl(request.getFacebookUrl());
        shop.setLat(request.getLat());
        shop.setLng(request.getLng());
        shop.setLocationSource(request.getLocationSource());
        shop.setLocationAccuracyM(request.getLocationAccuracyM());
    }

    private void applyImageUpdates(Shop shop, ShopProfileUpdateRequest request) {
        MultipartFile imageFile = request.getAvatar();
        if (imageFile != null && !imageFile.isEmpty()) {
            shop.setImageUrl(fileUploadUtil.uploadShopImage(shop.getId(), imageFile));
        } else if (request.getImageUrl() != null) {
            shop.setImageUrl(normalizeImageStoragePath(request.getImageUrl()));
        }

        MultipartFile coverImageFile = request.getCover_img();
        if (coverImageFile != null && !coverImageFile.isEmpty()) {
            shop.setCoverImageUrl(fileUploadUtil.uploadShopCoverImage(shop.getId(), coverImageFile));
        } else if (request.getCoverImageUrl() != null) {
            shop.setCoverImageUrl(normalizeImageStoragePath(request.getCoverImageUrl()));
        }
    }

    private String normalizeImageStoragePath(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }
        return fileUploadUtil.normalizeShopImageStoragePath(imageUrl.trim());
    }

    private ShopDTO toDTO(Shop shop) {
        return ShopMapper.toDTO(shop);
    }
}
