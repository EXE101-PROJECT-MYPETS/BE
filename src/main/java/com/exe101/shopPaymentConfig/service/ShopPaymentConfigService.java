package com.exe101.shopPaymentConfig.service;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.shop.entity.ShopRole;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import com.exe101.shopPaymentConfig.dto.ShopPaymentConfigDTO;
import com.exe101.shopPaymentConfig.entity.ShopPaymentConfig;
import com.exe101.shopPaymentConfig.exception.ShopPaymentConfigAccessDenied;
import com.exe101.shopPaymentConfig.exception.ShopPaymentConfigDuplicate;
import com.exe101.shopPaymentConfig.exception.ShopPaymentConfigNotFound;
import com.exe101.shopPaymentConfig.mapper.ShopPaymentConfigMapper;
import com.exe101.shopPaymentConfig.repository.IShopPaymentConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopPaymentConfigService {

    private static final List<ShopRole> READ_ROLES = List.of(ShopRole.OWNER, ShopRole.MANAGER, ShopRole.STAFF);
    private static final List<ShopRole> MANAGE_ROLES = List.of(ShopRole.OWNER, ShopRole.MANAGER);

    private final IShopPaymentConfigRepository shopPaymentConfigRepository;
    private final IShopMemberRepository shopMemberRepository;

    public List<ShopPaymentConfigDTO> getAllByShopId(Long shopId, Boolean active) {
        assertActiveShopMember(shopId);

        List<ShopPaymentConfig> configs = active == null
                ? shopPaymentConfigRepository.findByShopIdOrderByActiveDescIdAsc(shopId)
                : shopPaymentConfigRepository.findByShopIdAndActiveOrderByIdAsc(shopId, active);

        return configs.stream().map(ShopPaymentConfigMapper::toDTO).toList();
    }

    public ShopPaymentConfigDTO getDefault(Long shopId) {
        assertActiveShopMember(shopId);

        return shopPaymentConfigRepository.findFirstByShopIdAndActiveTrueOrderByIdAsc(shopId)
                .map(ShopPaymentConfigMapper::toDTO)
                .orElseThrow(() -> new ShopPaymentConfigNotFound(
                        "ShopPaymentConfigActiveNotFound",
                        "Chưa có tài khoản ngân hàng đang bật cho shop"
                ));
    }

    public ShopPaymentConfigDTO getById(Long shopId, Long id) {
        assertActiveShopMember(shopId);

        return shopPaymentConfigRepository.findByIdAndShopId(id, shopId)
                .map(ShopPaymentConfigMapper::toDTO)
                .orElseThrow(() -> new ShopPaymentConfigNotFound(
                        "ShopPaymentConfigNotFound",
                        "Không tìm thấy cấu hình tài khoản ngân hàng"
                ));
    }

    @Transactional
    public ShopPaymentConfigDTO create(Long shopId, ShopPaymentConfigDTO dto) {
        assertCanManagePaymentConfig(shopId);
        assertBankAccountNotDuplicated(shopId, dto, null);

        ShopPaymentConfig entity = ShopPaymentConfigMapper.toEntity(dto);
        entity.setShopId(shopId);
        if (entity.getActive() == null) {
            entity.setActive(true);
        }
        if (Boolean.TRUE.equals(entity.getActive())) {
            shopPaymentConfigRepository.deactivateOtherActiveConfigs(shopId, null);
        }

        return ShopPaymentConfigMapper.toDTO(shopPaymentConfigRepository.save(entity));
    }

    @Transactional
    public ShopPaymentConfigDTO update(Long shopId, Long id, ShopPaymentConfigDTO dto) {
        assertCanManagePaymentConfig(shopId);
        assertBankAccountNotDuplicated(shopId, dto, id);

        ShopPaymentConfig entity = shopPaymentConfigRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ShopPaymentConfigNotFound(
                        "ShopPaymentConfigNotFound",
                        "Không tìm thấy cấu hình tài khoản ngân hàng"
                ));

        if (Boolean.TRUE.equals(dto.getActive())) {
            shopPaymentConfigRepository.deactivateOtherActiveConfigs(shopId, id);
        }
        ShopPaymentConfigMapper.updateEntity(entity, dto);
        return ShopPaymentConfigMapper.toDTO(shopPaymentConfigRepository.save(entity));
    }

    @Transactional
    public void delete(Long shopId, Long id) {
        assertCanManagePaymentConfig(shopId);

        ShopPaymentConfig entity = shopPaymentConfigRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ShopPaymentConfigNotFound(
                        "ShopPaymentConfigNotFound",
                        "Không tìm thấy cấu hình tài khoản ngân hàng"
                ));
        shopPaymentConfigRepository.delete(entity);
    }

    private void assertBankAccountNotDuplicated(Long shopId, ShopPaymentConfigDTO dto, Long excludedId) {
        String bankCode = trim(dto.getBankCode());
        String accountNumber = trim(dto.getAccountNumber());
        boolean duplicated = excludedId == null
                ? shopPaymentConfigRepository.existsByShopIdAndBankCodeAndAccountNumber(
                        shopId,
                        bankCode,
                        accountNumber
                )
                : shopPaymentConfigRepository.existsByShopIdAndBankCodeAndAccountNumberAndIdNot(
                        shopId,
                        bankCode,
                        accountNumber,
                        excludedId
                );

        if (duplicated) {
            throw new ShopPaymentConfigDuplicate(
                    "ShopPaymentConfigDuplicate",
                    "Tài khoản ngân hàng này đã tồn tại trong shop"
            );
        }
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
            throw new ShopPaymentConfigAccessDenied(
                    "ShopPaymentConfigAccessDenied",
                    "Bạn không có quyền xem cấu hình thanh toán của shop này"
            );
        }
    }

    private void assertCanManagePaymentConfig(Long shopId) {
        Long userId = getCurrentUserId();
        boolean allowed = shopMemberRepository.existsByShopIdAndUserIdAndRoleInAndStatus(
                shopId,
                userId,
                MANAGE_ROLES,
                MemberStatus.ACTIVE
        );

        if (!allowed) {
            throw new ShopPaymentConfigAccessDenied(
                    "ShopPaymentConfigAccessDenied",
                    "Chỉ chủ shop hoặc quản lý mới được cấu hình tài khoản ngân hàng"
            );
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new ShopPaymentConfigAccessDenied(
                    "ShopPaymentConfigAccessDenied",
                    "Bạn cần đăng nhập để thao tác với cấu hình thanh toán"
            );
        }
        return userPrincipal.getUser().getId();
    }

    private String trim(String value) {
        return value != null ? value.trim() : null;
    }
}
