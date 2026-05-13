package com.exe101.service_shop.service;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.common.IService;
import com.exe101.common.ScrollResponse;
import com.exe101.file.FileUploadUtil;
import com.exe101.service_shop.dto.ServiceCreateRequest;
import com.exe101.service_shop.dto.ServiceDTO;
import com.exe101.service_shop.entity.Service;
import com.exe101.service_shop.exception.ServiceAccessDenied;
import com.exe101.service_shop.exception.ServiceDuplicate;
import com.exe101.service_shop.exception.ServiceNotFound;
import com.exe101.service_shop.mapper.ServiceMapper;
import com.exe101.service_shop.repository.IServiceRepository;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceService implements IService<Service, ServiceDTO, Long> {

    private static final int MAX_SCROLL_SIZE = 50;

    private final IServiceRepository serviceRepository;
    private final IShopMemberRepository shopMemberRepository;
    private final FileUploadUtil fileUploadUtil;

    @Override
    public List<ServiceDTO> getAll() {
        return serviceRepository.findAll().stream().map(this::toDTO).toList();
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

        List<Service> services = serviceRepository.findForScroll(
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
                .map(this::toDTO)
                .toList();
        Long nextCursor = hasNext && !content.isEmpty()
                ? content.get(content.size() - 1).getId()
                : null;

        return ScrollResponse.of(content, normalizedSize, nextCursor, hasNext);
    }

    @Override
    public ServiceDTO getById(Long id) {
        return serviceRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ServiceNotFound("ServiceNotFound", "Không tìm thấy dịch vụ"));
    }

    @Override
    public ServiceDTO create(ServiceDTO dto) {
        assertCanCreateService(dto.getShopId());
        assertServiceNameNotDuplicated(dto.getShopId(), dto.getName(), null);
        Service entity = ServiceMapper.toEntity(dto);
        entity.setImageUrl(normalizeImageUrl(dto.getImageUrl()));
        return toDTO(serviceRepository.save(entity));
    }

    public ServiceDTO create(Long shopId, ServiceCreateRequest request) {
        assertCanCreateService(shopId);
        assertServiceNameNotDuplicated(shopId, request.getName(), null);

        Service entity = new Service();
        entity.setShopId(shopId);
        applyWriteData(entity, request);
        entity.setImageUrl(normalizeImageUrl(request.getImageUrl()));

        Service saved = serviceRepository.save(entity);
        if (request.getImageUrlPreview() != null && !request.getImageUrlPreview().isEmpty()) {
            saved.setImageUrl(fileUploadUtil.uploadServiceImage(shopId, saved.getId(), request.getImageUrlPreview()));
            saved = serviceRepository.save(saved);
        }

        return toDTO(saved);
    }

    @Override
    public ServiceDTO update(Long id, ServiceDTO dto) {
        Service entity = serviceRepository.findById(id)
                .orElseThrow(() -> new ServiceNotFound("ServiceNotFound", "Không tìm thấy dịch vụ"));
        assertServiceNameNotDuplicated(dto.getShopId(), dto.getName(), id);
        applyWriteData(entity, dto.getShopId(), dto.getName(), dto.getDurationMin(), dto.getBasePrice(), dto.getCategoryId(), dto.getActive());
        entity.setImageUrl(normalizeImageUrl(dto.getImageUrl()));
        return toDTO(serviceRepository.save(entity));
    }

    public ServiceDTO update(Long id, Long shopId, ServiceCreateRequest request) {
        Service entity = serviceRepository.findById(id)
                .orElseThrow(() -> new ServiceNotFound("ServiceNotFound", "Không tìm thấy dịch vụ"));
        assertServiceNameNotDuplicated(shopId, request.getName(), id);

        applyWriteData(
                entity,
                shopId,
                request.getName(),
                request.getDurationMin(),
                request.getBasePrice(),
                request.getCategoryId(),
                request.getActive()
        );

        MultipartFile imageUrlPreview = request.getImageUrlPreview();
        if (imageUrlPreview != null && !imageUrlPreview.isEmpty()) {
            entity.setImageUrl(fileUploadUtil.uploadServiceImage(shopId, entity.getId(), imageUrlPreview));
        } else if (request.getImageUrl() != null) {
            entity.setImageUrl(normalizeImageUrl(request.getImageUrl()));
        }

        return toDTO(serviceRepository.save(entity));
    }

    private void applyWriteData(Service entity, ServiceCreateRequest request) {
        applyWriteData(
                entity,
                entity.getShopId(),
                request.getName(),
                request.getDurationMin(),
                request.getBasePrice(),
                request.getCategoryId(),
                request.getActive()
        );
    }

    private void applyWriteData(
            Service entity,
            Long shopId,
            String name,
            Integer durationMin,
            Long basePrice,
            Long categoryId,
            Boolean active
    ) {
        entity.setShopId(shopId);
        entity.setName(name);
        entity.setDurationMin(durationMin);
        entity.setBasePrice(basePrice);
        entity.setCategoryId(categoryId);
        entity.setActive(active != null ? active : Boolean.TRUE);
    }

    private String normalizeImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }
        return fileUploadUtil.normalizeServiceImagePath(imageUrl.trim());
    }

    private ServiceDTO toDTO(Service service) {
        ServiceDTO dto = ServiceMapper.toDTO(service);
        dto.setImageUrl(normalizeImageUrl(dto.getImageUrl()));
        return dto;
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
        boolean allowed = shopMemberRepository.existsByShopIdAndUserIdAndStatus(
                shopId,
                userId,
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
