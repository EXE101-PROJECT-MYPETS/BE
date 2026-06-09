package com.exe101.service_shop.service;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.common.ScrollResponse;
import com.exe101.file.FileUploadUtil;
import com.exe101.serviceReview.entity.ServiceReview;
import com.exe101.serviceReview.repository.IServiceReviewRepository;
import com.exe101.service_shop.dto.ServiceCreateRequest;
import com.exe101.service_shop.dto.ServiceDetailDTO;
import com.exe101.service_shop.dto.ServiceDetailReviewDTO;
import com.exe101.service_shop.dto.ServiceDetailReviewUserDTO;
import com.exe101.service_shop.dto.ServiceWriteDTO;
import com.exe101.service_shop.entity.Service;
import com.exe101.service_shop.entity.ServiceCategory;
import com.exe101.service_shop.entity.ServiceType;
import com.exe101.service_shop.entity.VeterinaryServiceType;
import com.exe101.service_shop.exception.ServiceAccessDenied;
import com.exe101.service_shop.exception.ServiceDuplicate;
import com.exe101.service_shop.exception.ServiceNotFound;
import com.exe101.service_shop.exception.ServiceValidationException;
import com.exe101.service_shop.mapper.ServiceMapper;
import com.exe101.service_shop.repository.IServiceCategoryRepository;
import com.exe101.service_shop.repository.IServiceRepository;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import com.exe101.vaccine.repository.IVaccineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceService {

    private static final int MAX_SCROLL_SIZE = 50;

    private final IServiceRepository serviceRepository;
    private final IServiceCategoryRepository serviceCategoryRepository;
    private final IShopMemberRepository shopMemberRepository;
    private final FileUploadUtil fileUploadUtil;
    private final IVaccineRepository vaccineRepository;
    private final IServiceReviewRepository serviceReviewRepository;

    public List<ServiceDetailDTO> getAll() {
        return serviceRepository.findAll().stream().map(this::toDetailDTO).toList();
    }

    public ScrollResponse<ServiceDetailDTO> getAllForScroll(
            Long shopId,
            String search,
            Long categoryId,
            ServiceType serviceType,
            VeterinaryServiceType veterinaryServiceType,
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
                serviceType,
                veterinaryServiceType,
                active,
                normalizedCursor,
                PageRequest.of(0, normalizedSize + 1)
        );

        boolean hasNext = services.size() > normalizedSize;
        List<ServiceDetailDTO> content = services.stream()
                .limit(normalizedSize)
                .map(this::toDetailDTO)
                .toList();
        Long nextCursor = hasNext && !content.isEmpty()
                ? content.get(content.size() - 1).getId()
                : null;

        return ScrollResponse.of(content, normalizedSize, nextCursor, hasNext);
    }

    public ServiceDetailDTO getById(Long id) {
        return serviceRepository.findDetailById(id)
                .map(this::toDetailDTO)
                .orElseThrow(() -> new ServiceNotFound("ServiceNotFound", "Không tìm thấy dịch vụ"));
    }

    public ServiceDetailDTO create(Long shopId, ServiceWriteDTO dto) {
        assertCanCreateService(shopId);
        assertServiceNameNotDuplicated(shopId, dto.getName(), null);
        validateVeterinaryService(dto.getServiceType(), dto.getVeterinaryServiceType(), dto.getVaccineId());
        validateCategoryType(shopId, dto.getCategoryId(), dto.getServiceType());

        Service entity = ServiceMapper.toEntity(shopId, dto);
        entity.setImageUrl(normalizeImageUrl(dto.getImageUrl()));

        Service saved = serviceRepository.save(entity);
        return getById(saved.getId());
    }

    public ServiceDetailDTO create(Long shopId, ServiceCreateRequest request) {
        assertCanCreateService(shopId);
        assertServiceNameNotDuplicated(shopId, request.getName(), null);
        validateVeterinaryService(request.getServiceType(), request.getVeterinaryServiceType(), request.getVaccineId());
        validateCategoryType(shopId, request.getCategoryId(), request.getServiceType());

        Service entity = new Service();
        entity.setShopId(shopId);
        applyWriteData(entity, request);
        entity.setImageUrl(normalizeImageUrl(request.getImageUrl()));

        Service saved = serviceRepository.save(entity);
        if (request.getImageUrlPreview() != null && !request.getImageUrlPreview().isEmpty()) {
            saved.setImageUrl(fileUploadUtil.uploadServiceImage(shopId, saved.getId(), request.getImageUrlPreview()));
            saved = serviceRepository.save(saved);
        }

        return getById(saved.getId());
    }

    public ServiceDetailDTO update(Long id, Long shopId, ServiceWriteDTO dto) {
        Service entity = serviceRepository.findById(id)
                .orElseThrow(() -> new ServiceNotFound("ServiceNotFound", "Không tìm thấy dịch vụ"));
        assertServiceNameNotDuplicated(shopId, dto.getName(), id);
        validateVeterinaryService(dto.getServiceType(), dto.getVeterinaryServiceType(), dto.getVaccineId());
        validateCategoryType(shopId, dto.getCategoryId(), dto.getServiceType());

        applyWriteData(
                entity,
                shopId,
                dto.getName(),
                dto.getDurationMin(),
                dto.getBasePrice(),
                dto.getCategoryId(),
                dto.getServiceType(),
                dto.getVeterinaryServiceType(),
                dto.getVaccineId(),
                dto.getActive()
        );
        entity.setImageUrl(normalizeImageUrl(dto.getImageUrl()));

        Service saved = serviceRepository.save(entity);
        return getById(saved.getId());
    }

    public ServiceDetailDTO update(Long id, Long shopId, ServiceCreateRequest request) {
        Service entity = serviceRepository.findById(id)
                .orElseThrow(() -> new ServiceNotFound("ServiceNotFound", "Không tìm thấy dịch vụ"));
        assertServiceNameNotDuplicated(shopId, request.getName(), id);
        validateVeterinaryService(request.getServiceType(), request.getVeterinaryServiceType(), request.getVaccineId());
        validateCategoryType(shopId, request.getCategoryId(), request.getServiceType());

        applyWriteData(
                entity,
                shopId,
                request.getName(),
                request.getDurationMin(),
                request.getBasePrice(),
                request.getCategoryId(),
                request.getServiceType(),
                request.getVeterinaryServiceType(),
                request.getVaccineId(),
                request.getActive()
        );

        MultipartFile imageUrlPreview = request.getImageUrlPreview();
        if (imageUrlPreview != null && !imageUrlPreview.isEmpty()) {
            entity.setImageUrl(fileUploadUtil.uploadServiceImage(shopId, entity.getId(), imageUrlPreview));
        } else if (request.getImageUrl() != null) {
            entity.setImageUrl(normalizeImageUrl(request.getImageUrl()));
        }

        Service saved = serviceRepository.save(entity);
        return getById(saved.getId());
    }

    private void applyWriteData(Service entity, ServiceCreateRequest request) {
        applyWriteData(
                entity,
                entity.getShopId(),
                request.getName(),
                request.getDurationMin(),
                request.getBasePrice(),
                request.getCategoryId(),
                request.getServiceType(),
                request.getVeterinaryServiceType(),
                request.getVaccineId(),
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
            ServiceType serviceType,
            VeterinaryServiceType veterinaryServiceType,
            Long vaccineId,
            Boolean active
    ) {
        entity.setShopId(shopId);
        entity.setName(name);
        entity.setDurationMin(durationMin);
        entity.setBasePrice(basePrice);
        entity.setCategoryId(categoryId);
        entity.setServiceType(serviceType != null ? serviceType : ServiceType.GENERAL);
        entity.setVeterinaryServiceType(veterinaryServiceType);
        entity.setVaccineId(vaccineId);
        entity.setActive(active != null ? active : Boolean.TRUE);
    }

    private void validateVeterinaryService(
            ServiceType serviceType,
            VeterinaryServiceType veterinaryServiceType,
            Long vaccineId
    ) {
        ServiceType normalizedType = serviceType != null ? serviceType : ServiceType.GENERAL;
        if (normalizedType == ServiceType.GENERAL) {
            if (veterinaryServiceType != null || vaccineId != null) {
                throw new ServiceValidationException(
                        "ServiceTypeInvalid",
                        "Dịch vụ thường không được có thông tin thú y hoặc vaccine"
                );
            }
            return;
        }

        if (veterinaryServiceType == null) {
            throw new ServiceValidationException(
                    "VeterinaryServiceTypeRequired",
                    "Dịch vụ thú y phải có loại dịch vụ thú y"
            );
        }

        if (veterinaryServiceType == VeterinaryServiceType.VACCINATION) {
            if (vaccineId == null) {
                throw new ServiceValidationException(
                        "VaccineRequired",
                        "Dịch vụ tiêm vaccine phải gắn vaccine"
                );
            }
            if (!vaccineRepository.existsById(vaccineId)) {
                throw new ServiceValidationException(
                        "VaccineNotFound",
                        "Không tìm thấy vaccine được gắn cho dịch vụ"
                );
            }
            return;
        }

        if (vaccineId != null) {
            throw new ServiceValidationException(
                    "VaccineOnlyForVaccination",
                    "Chỉ dịch vụ tiêm vaccine mới được gắn vaccine"
            );
        }
    }

    private void validateCategoryType(Long shopId, Long categoryId, ServiceType serviceType) {
        if (categoryId == null) {
            return;
        }

        ServiceType normalizedType = serviceType != null ? serviceType : ServiceType.GENERAL;
        ServiceCategory category = serviceCategoryRepository.findByIdAndShopId(categoryId, shopId)
                .orElseThrow(() -> new ServiceValidationException(
                        "ServiceCategoryNotFound",
                        "Không tìm thấy nhóm dịch vụ trong shop hiện tại"
                ));

        if (category.getServiceType() != normalizedType) {
            throw new ServiceValidationException(
                    "ServiceCategoryTypeMismatch",
                    normalizedType == ServiceType.VETERINARY
                            ? "Dịch vụ thú y phải dùng nhóm dịch vụ thú y"
                            : "Dịch vụ spa phải dùng nhóm dịch vụ spa"
            );
        }
    }

    private String normalizeImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }
        return fileUploadUtil.normalizeServiceImagePath(imageUrl.trim());
    }

    private ServiceDetailDTO toDetailDTO(Service service) {
        ServiceDetailDTO dto = ServiceMapper.toDetailDTO(service);
        dto.setImageUrl(normalizeImageUrl(dto.getImageUrl()));
        dto.setShopImageUrl(fileUploadUtil.normalizeShopImagePath(dto.getShopImageUrl()));
        List<ServiceReview> reviews = serviceReviewRepository.findByShopIdAndServiceIdOrderByIdDesc(
                service.getShopId(),
                service.getId()
        );
        dto.setReviews(reviews.stream()
                .map(this::toDetailReviewDTO)
                .toList());
        dto.setRating(reviews.stream()
                .mapToInt(ServiceReview::getRating)
                .average()
                .orElse(0D));
        dto.setRatingCount((long) reviews.size());
        return dto;
    }

    private ServiceDetailReviewDTO toDetailReviewDTO(ServiceReview review) {
        return new ServiceDetailReviewDTO(
                review.getId(),
                review.getRating(),
                review.getComment(),
                new ServiceDetailReviewUserDTO(
                        review.getCustomerId(),
                        review.getCustomer() != null ? review.getCustomer().getFullName() : null
                ),
                review.getCreatedAt()
        );
    }

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
