package com.exe101.service_shop.service;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.common.IService;
import com.exe101.common.ScrollResponse;
import com.exe101.service_shop.dto.ServiceDTO;
import com.exe101.service_shop.exception.ServiceAccessDenied;
import com.exe101.service_shop.exception.ServiceDuplicate;
import com.exe101.service_shop.exception.ServiceNotFound;
import com.exe101.service_shop.mapper.ServiceMapper;
import com.exe101.service_shop.repository.IServiceRepository;
import com.exe101.shop.entity.ShopRole;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceService implements IService<com.exe101.service_shop.entity.Service, ServiceDTO, Long> {

    private static final List<ShopRole> SERVICE_CREATE_ROLES = List.of(ShopRole.OWNER, ShopRole.MANAGER);
    private static final int MAX_SCROLL_SIZE = 50;

    private final IServiceRepository serviceRepository;
    private final IShopMemberRepository shopMemberRepository;

    @Override
    public List<ServiceDTO> getAll() {
        return serviceRepository.findAll().stream().map(ServiceMapper::toDTO).toList();
    }

    public ScrollResponse<ServiceDTO> getAllForScroll(
            Long shopId,
            String search,
            Long categoryId,
            Boolean active,
            Long cursor,
            int size
    ) {
        int normalizedSize = Math.min(Math.max(size, 1), MAX_SCROLL_SIZE);
        Long normalizedCursor = cursor != null && cursor > 0 ? cursor : null;
        String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;

        List<com.exe101.service_shop.entity.Service> services = serviceRepository.findForScroll(
                shopId,
                normalizedSearch,
                categoryId,
                active,
                normalizedCursor,
                PageRequest.of(0, normalizedSize + 1)
        );

        boolean hasNext = services.size() > normalizedSize;
        List<ServiceDTO> content = services.stream()
                .limit(normalizedSize)
                .map(ServiceMapper::toDTO)
                .toList();
        Long nextCursor = hasNext && !content.isEmpty()
                ? content.get(content.size() - 1).getId()
                : null;

        return ScrollResponse.of(content, normalizedSize, nextCursor, hasNext);
    }

    @Override
    public ServiceDTO getById(Long id) {
        return serviceRepository.findById(id)
                .map(ServiceMapper::toDTO)
                .orElseThrow(() -> new ServiceNotFound("ServiceNotFound", "Không tìm thấy dịch vụ"));
    }

    @Override
    public ServiceDTO create(ServiceDTO dto) {
        assertCanCreateService(dto.getShopId());
        assertServiceNameNotDuplicated(dto.getShopId(), dto.getName(), null);
        return ServiceMapper.toDTO(serviceRepository.save(ServiceMapper.toEntity(dto)));
    }

    @Override
    public ServiceDTO update(Long id, ServiceDTO dto) {
        com.exe101.service_shop.entity.Service entity = serviceRepository.findById(id)
                .orElseThrow(() -> new ServiceNotFound("ServiceNotFound", "Không tìm thấy dịch vụ"));
        assertServiceNameNotDuplicated(dto.getShopId(), dto.getName(), id);
        entity.setShopId(dto.getShopId());
        entity.setName(dto.getName());
        entity.setDurationMin(dto.getDurationMin());
        entity.setBasePrice(dto.getBasePrice());
        entity.setCategoryId(dto.getCategoryId());
        entity.setActive(dto.getActive() != null ? dto.getActive() : Boolean.TRUE);
        return ServiceMapper.toDTO(serviceRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!serviceRepository.existsById(id)) {
            throw new ServiceNotFound("ServiceNotFound", "Không tìm thấy dịch vụ");
        }
        serviceRepository.deleteById(id);
    }

    private void assertCanCreateService(Long shopId) {
        Long userId = getCurrentUserId();
        boolean allowed = shopMemberRepository.existsByShopIdAndUserIdAndRoleInAndStatus(
                shopId,
                userId,
                SERVICE_CREATE_ROLES,
                MemberStatus.ACTIVE
        );

        if (!allowed) {
            throw new ServiceAccessDenied(
                    "ServiceAccessDenied",
                    "Chỉ chủ shop hoặc quản lý mới được tạo dịch vụ"
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

    private void assertServiceNameNotDuplicated(Long shopId, String name, Long excludedId) {
        boolean duplicated = excludedId == null
                ? serviceRepository.existsByShopIdAndName(shopId, name)
                : serviceRepository.existsByShopIdAndNameAndIdNot(shopId, name, excludedId);

        if (duplicated) {
            throw new ServiceDuplicate(
                    "ServiceDuplicate",
                    "Tên dịch vụ đã tồn tại trong shop"
            );
        }
    }
}
