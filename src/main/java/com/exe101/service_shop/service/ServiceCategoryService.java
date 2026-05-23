package com.exe101.service_shop.service;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.service_shop.dto.ServiceCategoryDTO;
import com.exe101.service_shop.entity.ServiceCategory;
import com.exe101.service_shop.entity.ServiceType;
import com.exe101.service_shop.exception.ServiceAccessDenied;
import com.exe101.service_shop.exception.ServiceCategoryDuplicate;
import com.exe101.service_shop.exception.ServiceCategoryNotFound;
import com.exe101.service_shop.exception.ServiceValidationException;
import com.exe101.service_shop.mapper.ServiceCategoryMapper;
import com.exe101.service_shop.repository.IServiceCategoryRepository;
import com.exe101.service_shop.repository.IServiceRepository;
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

    private final IServiceCategoryRepository serviceCategoryRepository;
    private final IServiceRepository serviceRepository;
    private final IShopMemberRepository shopMemberRepository;

    public List<ServiceCategoryDTO> getAllByShop(Long shopId, ServiceType serviceType, Boolean active) {
        List<ServiceCategory> categories;
        if (serviceType == null) {
            categories = active == null
                    ? serviceCategoryRepository.findByShopIdOrderBySortOrderAscNameAsc(shopId)
                    : serviceCategoryRepository.findByShopIdAndActiveOrderBySortOrderAscNameAsc(shopId, active);
        } else {
            categories = active == null
                    ? serviceCategoryRepository.findByShopIdAndServiceTypeOrderBySortOrderAscNameAsc(shopId, serviceType)
                    : serviceCategoryRepository.findByShopIdAndServiceTypeAndActiveOrderBySortOrderAscNameAsc(shopId, serviceType, active);
        }

        return categories.stream().map(ServiceCategoryMapper::toDTO).toList();
    }

    public ServiceCategoryDTO getById(Long shopId, Long id) {
        return serviceCategoryRepository.findByIdAndShopId(id, shopId)
                .map(ServiceCategoryMapper::toDTO)
                .orElseThrow(() -> new ServiceCategoryNotFound("ServiceCategoryNotFound", "Không tìm thấy nhóm dịch vụ"));
    }

    public ServiceCategoryDTO create(Long shopId, ServiceCategoryDTO dto) {
        assertCanManageCategory(shopId);
        ServiceType normalizedType = dto.getServiceType() != null ? dto.getServiceType() : ServiceType.GENERAL;
        dto.setServiceType(normalizedType);
        assertCategoryNameNotDuplicated(shopId, dto.getName(), normalizedType, null);

        ServiceCategory entity = ServiceCategoryMapper.toEntity(dto);
        entity.setShopId(shopId);
        return ServiceCategoryMapper.toDTO(serviceCategoryRepository.save(entity));
    }

    public ServiceCategoryDTO update(Long shopId, Long id, ServiceCategoryDTO dto) {
        assertCanManageCategory(shopId);

        ServiceCategory entity = serviceCategoryRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ServiceCategoryNotFound("ServiceCategoryNotFound", "Không tìm thấy nhóm dịch vụ"));
        ServiceType normalizedType = dto.getServiceType() != null ? dto.getServiceType() : entity.getServiceType();
        dto.setServiceType(normalizedType);
        assertCategoryNameNotDuplicated(shopId, dto.getName(), normalizedType, id);
        ServiceCategoryMapper.updateEntity(entity, dto);
        return ServiceCategoryMapper.toDTO(serviceCategoryRepository.save(entity));
    }

    public void delete(Long shopId, Long id) {
        assertCanManageCategory(shopId);

        ServiceCategory entity = serviceCategoryRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ServiceCategoryNotFound("ServiceCategoryNotFound", "Không tìm thấy nhóm dịch vụ"));
        if (serviceRepository.existsByShopIdAndCategoryId(shopId, id)) {
            throw new ServiceValidationException(
                    "ServiceCategoryInUse",
                    "Không thể xóa nhóm dịch vụ vì đang có dịch vụ liên kết"
            );
        }
        entity.setActive(false);
        serviceCategoryRepository.save(entity);
    }

    private void assertCanManageCategory(Long shopId) {
        Long userId = getCurrentUserId();
        boolean allowed = shopMemberRepository.existsByShopIdAndUserIdAndStatus(
                shopId,
                userId,
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

    private void assertCategoryNameNotDuplicated(Long shopId, String name, ServiceType serviceType, Long excludedId) {
        boolean duplicated = excludedId == null
                ? serviceCategoryRepository.existsByShopIdAndNameAndServiceType(shopId, name, serviceType)
                : serviceCategoryRepository.existsByShopIdAndNameAndServiceTypeAndIdNot(shopId, name, serviceType, excludedId);

        if (duplicated) {
            throw new ServiceCategoryDuplicate(
                    "ServiceCategoryDuplicate",
                    "Tên nhóm dịch vụ đã tồn tại trong shop"
            );
        }
    }
}
