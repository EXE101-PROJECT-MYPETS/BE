package com.exe101.shopGhtkConfig.service;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.shop.entity.ShopRole;
import com.exe101.shopGhtkConfig.dto.ShopGhtkConfigDTO;
import com.exe101.shopGhtkConfig.dto.ShopGhtkConfigTestResponse;
import com.exe101.shopGhtkConfig.entity.ShopGhtkConfig;
import com.exe101.shopGhtkConfig.exception.ShopGhtkConfigAccessDenied;
import com.exe101.shopGhtkConfig.exception.ShopGhtkConfigNotFound;
import com.exe101.shopGhtkConfig.exception.ShopGhtkConfigValidationException;
import com.exe101.shopGhtkConfig.mapper.ShopGhtkConfigMapper;
import com.exe101.shopGhtkConfig.repository.IShopGhtkConfigRepository;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopGhtkConfigService {

    private static final List<ShopRole> READ_ROLES = List.of(ShopRole.OWNER, ShopRole.MANAGER, ShopRole.STAFF);
    private static final List<ShopRole> MANAGE_ROLES = List.of(ShopRole.OWNER, ShopRole.MANAGER);

    private final IShopGhtkConfigRepository shopGhtkConfigRepository;
    private final IShopMemberRepository shopMemberRepository;
    private final GhtkConfigCryptoService cryptoService;

    public ShopGhtkConfigDTO getByShopId(Long shopId) {
        assertActiveShopMember(shopId);
        return shopGhtkConfigRepository.findByShopId(shopId)
                .map(ShopGhtkConfigMapper::toDTO)
                .orElseThrow(() -> new ShopGhtkConfigNotFound(
                        "ShopGhtkConfigNotFound",
                        "Chưa có cấu hình GHTK cho shop"
                ));
    }

    @Transactional
    public ShopGhtkConfigDTO save(Long shopId, ShopGhtkConfigDTO dto) {
        assertCanManageGhtkConfig(shopId);

        ShopGhtkConfig entity = shopGhtkConfigRepository.findByShopId(shopId)
                .orElseGet(() -> {
                    ShopGhtkConfig newEntity = new ShopGhtkConfig();
                    newEntity.setShopId(shopId);
                    return newEntity;
                });

        applyApiToken(entity, dto);
        ShopGhtkConfigMapper.updateEntity(entity, dto);
        if (Boolean.TRUE.equals(entity.getEnabled())) {
            assertConfigCanBeEnabled(entity);
        }

        return ShopGhtkConfigMapper.toDTO(shopGhtkConfigRepository.save(entity));
    }

    public ShopGhtkConfigTestResponse test(Long shopId) {
        assertCanManageGhtkConfig(shopId);

        ShopGhtkConfig entity = shopGhtkConfigRepository.findByShopId(shopId)
                .orElseThrow(() -> new ShopGhtkConfigNotFound(
                        "ShopGhtkConfigNotFound",
                        "Chưa có cấu hình GHTK cho shop"
                ));
        assertConfigCanBeEnabled(entity);

        return new ShopGhtkConfigTestResponse(
                true,
                "Cấu hình GHTK hợp lệ và token có thể giải mã"
        );
    }

    private void applyApiToken(ShopGhtkConfig entity, ShopGhtkConfigDTO dto) {
        String apiToken = dto.getApiToken();
        if (apiToken == null) {
            if (entity.getEncryptedApiToken() == null || entity.getEncryptedApiToken().isBlank()) {
                throw new ShopGhtkConfigValidationException(
                        "GhtkApiTokenRequired",
                        "Token API GHTK không được để trống"
                );
            }
            return;
        }
        if (apiToken.isBlank()) {
            throw new ShopGhtkConfigValidationException(
                    "GhtkApiTokenRequired",
                    "Token API GHTK không được để trống"
            );
        }
        entity.setEncryptedApiToken(cryptoService.encrypt(apiToken.trim()));
    }

    private void assertConfigCanBeEnabled(ShopGhtkConfig entity) {
        if (entity.getEncryptedApiToken() == null || entity.getEncryptedApiToken().isBlank()) {
            throw new ShopGhtkConfigValidationException(
                    "GhtkApiTokenRequired",
                    "Token API GHTK không được để trống"
            );
        }
        if (isBlank(entity.getPickName())
                || isBlank(entity.getPickTel())
                || isBlank(entity.getPickAddress())
                || isBlank(entity.getPickProvince())
                || isBlank(entity.getPickDistrict())) {
            throw new ShopGhtkConfigValidationException(
                    "GhtkPickupInfoRequired",
                    "Thông tin kho lấy hàng GHTK chưa đầy đủ"
            );
        }
        cryptoService.decrypt(entity.getEncryptedApiToken());
    }

    private void assertActiveShopMember(Long shopId) {
        Long userId = getCurrentUserId();
        boolean allowed = shopMemberRepository.existsByShopIdAndUserIdAndRoleInAndStatus(
                shopId,
                userId,
                READ_ROLES,
                MemberStatus.ACTIVE
        );

        if (!allowed) {
            throw new ShopGhtkConfigAccessDenied(
                    "ShopGhtkConfigAccessDenied",
                    "Bạn không có quyền xem cấu hình GHTK của shop này"
            );
        }
    }

    private void assertCanManageGhtkConfig(Long shopId) {
        Long userId = getCurrentUserId();
        boolean allowed = shopMemberRepository.existsByShopIdAndUserIdAndRoleInAndStatus(
                shopId,
                userId,
                MANAGE_ROLES,
                MemberStatus.ACTIVE
        );

        if (!allowed) {
            throw new ShopGhtkConfigAccessDenied(
                    "ShopGhtkConfigAccessDenied",
                    "Chỉ chủ shop hoặc quản lý mới được cấu hình GHTK"
            );
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new ShopGhtkConfigAccessDenied(
                    "ShopGhtkConfigAccessDenied",
                    "Bạn cần đăng nhập để thao tác với cấu hình GHTK"
            );
        }
        return userPrincipal.getUser().getId();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
