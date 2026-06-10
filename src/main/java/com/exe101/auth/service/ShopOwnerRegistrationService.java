package com.exe101.auth.service;

import com.exe101.auth.dto.ShopOwnerRegisterRequest;
import com.exe101.auth.dto.ShopOwnerRegistrationResponse;
import com.exe101.file.FileUploadUtil;
import com.exe101.shop.dto.ShopDTO;
import com.exe101.shop.entity.LocationSource;
import com.exe101.shop.entity.Shop;
import com.exe101.shop.entity.ShopRole;
import com.exe101.shop.entity.ShopStatus;
import com.exe101.shop.mapper.ShopMapper;
import com.exe101.shop.repository.IShopRepository;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.entity.ShopMember;
import com.exe101.shopMember.entity.ShopMemberId;
import com.exe101.shopMember.repository.IShopMemberRepository;
import com.exe101.user.entity.User;
import com.exe101.user.entity.UserRole;
import com.exe101.user.entity.UserStatus;
import com.exe101.user.exception.UserDuplicate;
import com.exe101.user.mapper.UserMapper;
import com.exe101.user.repository.IUserRepository;
import com.exe101.userCredential.entity.CredentialProvider;
import com.exe101.userCredential.entity.UserCredential;
import com.exe101.userCredential.repository.IUserCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class ShopOwnerRegistrationService {

    private final IUserRepository userRepository;
    private final IUserCredentialRepository credentialRepository;
    private final IShopRepository shopRepository;
    private final IShopMemberRepository shopMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final FileUploadUtil fileUploadUtil;

    @Transactional
    public ShopOwnerRegistrationResponse register(ShopOwnerRegisterRequest request) {
        validateUniqueUser(request.getEmail(), request.getPhone());

        User user = createShopUser(request);
        user = uploadAvatarIfPresent(user, request.getAvatarUrlPreview());

        createCredential(user, request.getPassword());
        Shop shop = createShop(request);
        createOwnerMembership(shop.getId(), user.getId());

        ShopDTO shopDto = ShopMapper.toDTO(shop);

        return new ShopOwnerRegistrationResponse(
                null,
                user.getRole(),
                null,
                userMapper.toDTO(user),
                shopDto,
                "Đăng ký shop thành công, vui lòng chờ admin duyệt"
        );
    }

    private void validateUniqueUser(String email, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new UserDuplicate("EmailUserDuplicate", "Email đã tồn tại");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new UserDuplicate("PhoneUserDuplicate", "Số điện thoại đã tồn tại");
        }
    }

    private User createShopUser(ShopOwnerRegisterRequest request) {
        User user = new User();
        LocalDateTime now = LocalDateTime.now();

        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setAge(request.getAge());
        user.setRole(UserRole.SHOP);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return userRepository.save(user);
    }

    private User uploadAvatarIfPresent(User user, MultipartFile avatarFile) {
        if (avatarFile == null || avatarFile.isEmpty()) {
            return user;
        }

        String avatarUrl = fileUploadUtil.uploadUserAvatar(user.getId(), avatarFile);
        user.setAvatarUrlPreview(avatarUrl);
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    private UserCredential createCredential(User user, String rawPassword) {
        UserCredential credential = new UserCredential();
        OffsetDateTime now = OffsetDateTime.now();

        credential.setUser(user);
        credential.setProvider(CredentialProvider.LOCAL);
        credential.setPasswordHash(passwordEncoder.encode(rawPassword));
        credential.setProviderUserId(null);
        credential.setCreatedAt(now);
        credential.setUpdatedAt(now);

        return credentialRepository.save(credential);
    }

    private Shop createShop(ShopOwnerRegisterRequest request) {
        Shop shop = new Shop();
        OffsetDateTime now = OffsetDateTime.now();

        shop.setName(request.getShopName());
        shop.setAddressText(request.getShopAddressText());
        shop.setLat(request.getLat());
        shop.setLng(request.getLng());
        shop.setLocationSource(
                request.getLocationSource() != null ? request.getLocationSource() : LocationSource.MANUAL
        );
        shop.setLocationAccuracyM(request.getLocationAccuracyM());
        shop.setLocationUpdatedAt(now);
        shop.setStatus(ShopStatus.PENDING_APPROVAL);

        return shopRepository.save(shop);
    }

    private void createOwnerMembership(Long shopId, Long userId) {
        ShopMember member = new ShopMember();

        member.setId(new ShopMemberId(shopId, userId));
        member.setRole(ShopRole.OWNER);
        member.setStatus(MemberStatus.INACTIVE);

        shopMemberRepository.save(member);
    }
}
