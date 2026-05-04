package com.exe101.service_shop.service;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.service_shop.dto.ServiceCategoryDTO;
import com.exe101.service_shop.entity.ServiceCategory;
import com.exe101.service_shop.exception.ServiceAccessDenied;
import com.exe101.service_shop.exception.ServiceCategoryDuplicate;
import com.exe101.service_shop.exception.ServiceCategoryNotFound;
import com.exe101.service_shop.mapper.ServiceCategoryMapper;
import com.exe101.service_shop.repository.IServiceCategoryRepository;
import com.exe101.shop.entity.ShopRole;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceCategoryService {

    private static final List<ShopRole> CATEGORY_MANAGE_ROLES = List.of(ShopRole.OWNER, ShopRole.MANAGER);

    private final IServiceCategoryRepository serviceCategoryRepository;
    private final IShopMemberRepository shopMemberRepository;

    public List<ServiceCategoryDTO> getAllByShop(Long shopId, Boolean active) {
        List<ServiceCategory> categories = active == null
                ? serviceCategoryRepository.findByShopIdOrderBySortOrderAscNameAsc(shopId)
                : serviceCategoryRepository.findByShopIdAndActiveOrderBySortOrderAscNameAsc(shopId, active);

        return categories.stream().map(ServiceCategoryMapper::toDTO).toList();
    }

    public ServiceCategoryDTO getById(Long shopId, Long id) {
        return serviceCategoryRepository.findByIdAndShopId(id, shopId)
                .map(ServiceCategoryMapper::toDTO)
                .orElseThrow(() -> new ServiceCategoryNotFound("ServiceCategoryNotFound", "Không tìm thấy nhóm dịch vụ"));
    }

    public ServiceCategoryDTO create(Long shopId, ServiceCategoryDTO dto) {
        assertCanManageCategory(shopId);
        assertCategoryNameNotDuplicated(shopId, dto.getName(), null);

        ServiceCategory entity = ServiceCategoryMapper.toEntity(dto);
        entity.setShopId(shopId);
        return ServiceCategoryMapper.toDTO(serviceCategoryRepository.save(entity));
    }

    public ServiceCategoryDTO update(Long shopId, Long id, ServiceCategoryDTO dto) {
        assertCanManageCategory(shopId);

        ServiceCategory entity = serviceCategoryRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ServiceCategoryNotFound("ServiceCategoryNotFound", "Không tìm thấy nhóm dịch vụ"));
        assertCategoryNameNotDuplicated(shopId, dto.getName(), id);
        ServiceCategoryMapper.updateEntity(entity, dto);
        return ServiceCategoryMapper.toDTO(serviceCategoryRepository.save(entity));
    }

    public void delete(Long shopId, Long id) {
        assertCanManageCategory(shopId);

        ServiceCategory entity = serviceCategoryRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ServiceCategoryNotFound("ServiceCategoryNotFound", "Không tìm thấy nhóm dịch vụ"));
        entity.setActive(false);
        serviceCategoryRepository.save(entity);
    }

    private void assertCanManageCategory(Long shopId) {
        Long userId = getCurrentUserId();
        boolean allowed = shopMemberRepository.existsByShopIdAndUserIdAndRoleInAndStatus(
                shopId,
                userId,
                CATEGORY_MANAGE_ROLES,
                MemberStatus.ACTIVE
        );

        if (!allowed) {
            throw new ServiceAccessDenied(
                    "ServiceAccessDenied",
                    "Chỉ chủ shop hoặc quản lý mới được quản lý nhóm dịch vụ"
            );
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new ServiceAccessDenied(
                    "ServiceAccessDenied",
                    "Yêu cầu người dùng đã đăng nhập"
            );
        }
        return userPrincipal.getUser().getId();
    }

    private void assertCategoryNameNotDuplicated(Long shopId, String name, Long excludedId) {
        boolean duplicated = excludedId == null
                ? serviceCategoryRepository.existsByShopIdAndName(shopId, name)
                : serviceCategoryRepository.existsByShopIdAndNameAndIdNot(shopId, name, excludedId);

        if (duplicated) {
            throw new ServiceCategoryDuplicate(
                    "ServiceCategoryDuplicate",
                    "Tên nhóm dịch vụ đã tồn tại trong shop"
            );
        }
    }
}
