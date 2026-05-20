package com.exe101.shop.service;

import com.exe101.common.IService;
import com.exe101.email.exception.EmailValidationException;
import com.exe101.email.service.EmailService;
import com.exe101.file.FileUploadUtil;
import com.exe101.shop.dto.ShopDTO;
import com.exe101.shop.dto.ShopProfileUpdateRequest;
import com.exe101.shop.dto.ShopRegistrationEmailRequest;
import com.exe101.shop.dto.ShopRegistrationEmailResponse;
import com.exe101.shop.entity.Shop;
import com.exe101.shop.entity.ShopRole;
import com.exe101.shop.entity.ShopStatus;
import com.exe101.shop.exception.ShopNotFound;
import com.exe101.shop.mapper.ShopMapper;
import com.exe101.shop.repository.IShopRepository;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.entity.ShopMember;
import com.exe101.shopMember.repository.IShopMemberRepository;
import com.exe101.subscription.service.SubscriptionService;
import com.exe101.user.dto.UserDTO;
import com.exe101.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopService implements IService<Shop, ShopDTO, Long> {

    private final IShopRepository shopRepository;
    private final IShopMemberRepository shopMemberRepository;
    private final FileUploadUtil fileUploadUtil;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final SubscriptionService subscriptionService;

    @Override
    @Transactional(readOnly = true)
    public List<ShopDTO> getAll() {
        return shopRepository.findAllByOrderByIdAsc().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShopDTO> getAllByStatus(ShopStatus status) {
        List<Shop> shops = status == null
                ? shopRepository.findAllByOrderByIdAsc()
                : shopRepository.findAllByStatusOrderByIdAsc(status);
        Map<Long, UserDTO> ownersByShopId = resolveOwnersByShopId(shops);
        return shops.stream()
                .map(shop -> toDTOWithOwner(shop, ownersByShopId.get(shop.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShopDTO getById(Long id) {
        return shopRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ShopNotFound("ShopNotFound", "Khong tim thay shop"));
    }

    @Transactional(readOnly = true)
    public ShopDTO getByIdWithOwner(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ShopNotFound("ShopNotFound", "Khong tim thay shop"));
        UserDTO owner = resolveOwnerByShopId(id);
        return toDTOWithOwner(shop, owner);
    }

    @Transactional(readOnly = true)
    public ShopRegistrationEmailResponse sendRegistrationEmail(
            Long shopId,
            ShopRegistrationEmailRequest request
    ) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFound("ShopNotFound", "Khong tim thay shop"));
        validatePendingApprovalShop(shop);
        UserDTO owner = resolveRequiredOwner(shopId);
        String ownerEmail = resolveRequiredOwnerEmail(owner);

        emailService.sendShopRegistrationMessage(
                ownerEmail,
                request.getTitle(),
                owner.getFullName(),
                shop.getName(),
                request.getContent()
        );

        return new ShopRegistrationEmailResponse(
                true,
                shopId,
                ownerEmail,
                "Da gui email toi shop dang ky"
        );
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

    @Transactional
    public ShopDTO approve(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ShopNotFound("ShopNotFound", "Khong tim thay shop"));
        UserDTO owner = resolveRequiredOwner(id);

        shop.setStatus(ShopStatus.ACTIVE);
        activateOwnerMemberships(id);

        Shop savedShop = shopRepository.save(shop);
        subscriptionService.createTrialIfAbsent(savedShop.getId());
        emailService.sendShopRegistrationApproved(
                resolveRequiredOwnerEmail(owner),
                owner.getFullName(),
                savedShop.getName()
        );

        return toDTOWithOwner(savedShop, owner);
    }

    @Transactional
    public ShopDTO reject(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ShopNotFound("ShopNotFound", "Khong tim thay shop"));
        UserDTO owner = resolveRequiredOwner(id);

        shop.setStatus(ShopStatus.REJECTED);
        deactivateShopMemberships(id);

        Shop savedShop = shopRepository.save(shop);
        emailService.sendShopRegistrationRejected(
                resolveRequiredOwnerEmail(owner),
                owner.getFullName(),
                savedShop.getName()
        );

        return toDTOWithOwner(savedShop, owner);
    }

    private void activateOwnerMemberships(Long shopId) {
        List<ShopMember> owners = shopMemberRepository.findByShopIdAndRole(shopId, ShopRole.OWNER);
        for (ShopMember owner : owners) {
            owner.setStatus(MemberStatus.ACTIVE);
        }
        shopMemberRepository.saveAll(owners);
    }

    private void deactivateShopMemberships(Long shopId) {
        List<ShopMember> members = shopMemberRepository.findByShopId(shopId);
        for (ShopMember member : members) {
            member.setStatus(MemberStatus.INACTIVE);
        }
        shopMemberRepository.saveAll(members);
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

    private ShopDTO toDTOWithOwner(Shop shop, UserDTO owner) {
        ShopDTO dto = toDTO(shop);
        dto.setOwner(owner);
        return dto;
    }

    private Map<Long, UserDTO> resolveOwnersByShopId(List<Shop> shops) {
        List<Long> shopIds = shops.stream()
                .map(Shop::getId)
                .filter(Objects::nonNull)
                .toList();
        if (shopIds.isEmpty()) {
            return Map.of();
        }

        return shopMemberRepository.findByShopIdInAndRoleWithUser(shopIds, ShopRole.OWNER).stream()
                .filter(member -> member.getShopId() != null && member.getUser() != null)
                .collect(Collectors.toMap(
                        ShopMember::getShopId,
                        member -> userMapper.toDTO(member.getUser()),
                        (first, ignored) -> first
                ));
    }

    private UserDTO resolveOwnerByShopId(Long shopId) {
        if (shopId == null) {
            return null;
        }

        return shopMemberRepository.findByShopIdInAndRoleWithUser(List.of(shopId), ShopRole.OWNER).stream()
                .filter(member -> member.getUser() != null)
                .map(member -> userMapper.toDTO(member.getUser()))
                .findFirst()
                .orElse(null);
    }

    private UserDTO resolveRequiredOwner(Long shopId) {
        UserDTO owner = resolveOwnerByShopId(shopId);
        if (owner == null) {
            throw new EmailValidationException(
                    "ShopOwnerEmailRecipientNotFound",
                    "Khong tim thay chu shop de gui email"
            );
        }
        return owner;
    }

    private String resolveRequiredOwnerEmail(UserDTO owner) {
        if (owner == null || !StringUtils.hasText(owner.getEmail())) {
            throw new EmailValidationException(
                    "ShopOwnerEmailRequired",
                    "Chu shop chua co email de nhan thong bao"
            );
        }
        return owner.getEmail().trim();
    }

    private void validatePendingApprovalShop(Shop shop) {
        if (shop.getStatus() != ShopStatus.PENDING_APPROVAL) {
            throw new EmailValidationException(
                    "ShopRegistrationEmailStatusInvalid",
                    "Chi co the gui email cho shop dang cho duyet"
            );
        }
    }
}
