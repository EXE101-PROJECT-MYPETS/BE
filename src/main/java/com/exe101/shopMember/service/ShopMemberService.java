package com.exe101.shopMember.service;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.shop.entity.ShopRole;
import com.exe101.shopMember.dto.ShopMemberCreateRequest;
import com.exe101.shopMember.dto.ShopMemberDTO;
import com.exe101.shopMember.dto.ShopMemberResetPasswordRequest;
import com.exe101.shopMember.dto.ShopMemberUpdateRequest;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.entity.ShopMember;
import com.exe101.shopMember.entity.ShopMemberId;
import com.exe101.shopMember.exception.ShopMemberAccessDenied;
import com.exe101.shopMember.exception.ShopMemberNotFound;
import com.exe101.shopMember.exception.ShopMemberValidationException;
import com.exe101.shopMember.repository.IShopMemberRepository;
import com.exe101.user.entity.User;
import com.exe101.user.entity.UserRole;
import com.exe101.user.entity.UserStatus;
import com.exe101.user.exception.UserDuplicate;
import com.exe101.user.repository.IUserRepository;
import com.exe101.userCredential.entity.CredentialProvider;
import com.exe101.userCredential.entity.UserCredential;
import com.exe101.userCredential.repository.IUserCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopMemberService {

    private final IShopMemberRepository shopMemberRepository;
    private final IUserRepository userRepository;
    private final IUserCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;

    public List<ShopMemberDTO> getActiveStaffByShop(Long shopId) {
        assertCanViewMembers(shopId);
        return shopMemberRepository.findByShopIdAndStatusForDisplay(
                shopId,
                MemberStatus.ACTIVE
        );
    }

    public List<ShopMemberDTO> getAllByShop(Long shopId, ShopRole role, MemberStatus status, String keyword) {
        assertCanViewMembers(shopId);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return shopMemberRepository.findByShopIdForDisplay(
                shopId,
                role,
                status,
                normalizedKeyword
        );
    }

    public ShopMemberDTO getByShopAndUserId(Long shopId, Long userId) {
        assertCanViewMembers(shopId);
        return shopMemberRepository.findDetailByShopIdAndUserId(shopId, userId)
                .orElseThrow(() -> new ShopMemberNotFound(
                        "ShopMemberNotFound",
                        "Khong tim thay tai khoan trong shop"
                ));
    }

    @Transactional
    public ShopMemberDTO create(Long shopId, ShopMemberCreateRequest request) {
        assertCanManageMembers(shopId);

        MemberStatus targetStatus = request.getStatus() != null ? request.getStatus() : MemberStatus.ACTIVE;
        if (targetStatus == MemberStatus.ACTIVE
                && shopMemberRepository.existsByShopIdAndStatus(shopId, MemberStatus.ACTIVE)) {
            throw new ShopMemberValidationException(
                    "ShopSingleActiveAccount",
                    "Shop chi duoc co mot tai khoan dang hoat dong"
            );
        }

        validateUniqueUser(request.getEmail(), request.getPhone());

        User user = createShopUser(request);
        createCredential(user, request.getPassword());

        ShopMember member = new ShopMember();
        member.setId(new ShopMemberId(shopId, user.getId()));
        member.setRole(ShopRole.OWNER);
        member.setStatus(targetStatus);

        shopMemberRepository.save(member);
        return getByShopAndUserId(shopId, user.getId());
    }

    @Transactional
    public ShopMemberDTO update(Long shopId, Long userId, ShopMemberUpdateRequest request) {
        if (request.getRole() == null && request.getStatus() == null) {
            throw new ShopMemberValidationException(
                    "ShopMemberUpdateEmpty",
                    "Can cung cap trang thai de cap nhat tai khoan shop"
            );
        }

        ShopMember actorMembership = assertCanManageMembers(shopId);
        ShopMember member = getMemberEntity(shopId, userId);
        Long currentUserId = actorMembership.getUserId();

        if (currentUserId.equals(userId)) {
            throw new ShopMemberValidationException(
                    "ShopMemberSelfUpdateNotAllowed",
                    "Khong the tu thay doi trang thai cua chinh minh"
            );
        }

        if (request.getStatus() != null) {
            member.setStatus(request.getStatus());
        }
        shopMemberRepository.save(member);
        return getByShopAndUserId(shopId, userId);
    }

    @Transactional
    public void resetPassword(Long shopId, Long userId, ShopMemberResetPasswordRequest request) {
        ShopMember actorMembership = assertCanManageMembers(shopId);
        getMemberEntity(shopId, userId);

        if (actorMembership.getUserId().equals(userId)) {
            throw new ShopMemberValidationException(
                    "ShopMemberSelfResetPasswordNotAllowed",
                    "Khong the dung API nay de tu dat lai mat khau cua chinh minh"
            );
        }

        UserCredential credential = credentialRepository.findById(userId)
                .orElseThrow(() -> new ShopMemberNotFound(
                        "ShopMemberCredentialNotFound",
                        "Khong tim thay thong tin dang nhap cua tai khoan"
                ));

        credential.setProvider(CredentialProvider.LOCAL);
        credential.setPasswordHash(passwordEncoder.encode(normalizeRequiredValue(request.getNewPassword())));
        credential.setUpdatedAt(OffsetDateTime.now());
        credentialRepository.save(credential);
    }

    @Transactional
    public void delete(Long shopId, Long userId) {
        ShopMember actorMembership = assertCanManageMembers(shopId);
        ShopMember member = getMemberEntity(shopId, userId);

        if (actorMembership.getUserId().equals(userId)) {
            throw new ShopMemberValidationException(
                    "ShopMemberSelfDeleteNotAllowed",
                    "Khong the tu xoa chinh minh khoi shop"
            );
        }

        member.setStatus(MemberStatus.REMOVED);
        shopMemberRepository.save(member);
    }

    private void validateUniqueUser(String email, String phone) {
        if (userRepository.existsByEmail(normalizeRequiredValue(email))) {
            throw new UserDuplicate("EmailUserDuplicate", "Email da ton tai");
        }
        if (userRepository.existsByPhone(normalizeRequiredValue(phone))) {
            throw new UserDuplicate("PhoneUserDuplicate", "So dien thoai da ton tai");
        }
    }

    private User createShopUser(ShopMemberCreateRequest request) {
        User user = new User();
        LocalDateTime now = LocalDateTime.now();

        user.setFullName(normalizeRequiredValue(request.getFullName()));
        user.setEmail(normalizeRequiredValue(request.getEmail()));
        user.setPhone(normalizeRequiredValue(request.getPhone()));
        user.setAddress(normalizeOptionalValue(request.getAddress()));
        user.setAge(request.getAge());
        user.setRole(UserRole.SHOP);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return userRepository.save(user);
    }

    private void createCredential(User user, String rawPassword) {
        UserCredential credential = new UserCredential();
        OffsetDateTime now = OffsetDateTime.now();

        credential.setUser(user);
        credential.setProvider(CredentialProvider.LOCAL);
        credential.setPasswordHash(passwordEncoder.encode(rawPassword));
        credential.setProviderUserId(null);
        credential.setCreatedAt(now);
        credential.setUpdatedAt(now);

        credentialRepository.save(credential);
    }

    private void assertCanViewMembers(Long shopId) {
        ShopMember membership = getCurrentMembership(shopId);
        if (membership.getStatus() != MemberStatus.ACTIVE) {
            throw new ShopMemberAccessDenied(
                    "ShopMemberAccessDenied",
                    "Tai khoan shop khong hoat dong"
            );
        }
    }

    private ShopMember assertCanManageMembers(Long shopId) {
        ShopMember membership = getCurrentMembership(shopId);
        if (membership.getStatus() != MemberStatus.ACTIVE) {
            throw new ShopMemberAccessDenied(
                    "ShopMemberAccessDenied",
                    "Tai khoan shop khong hoat dong"
            );
        }
        return membership;
    }

    private ShopMember getCurrentMembership(Long shopId) {
        Long currentUserId = getCurrentUserId();
        return shopMemberRepository.findByShopIdAndUserId(shopId, currentUserId)
                .orElseThrow(() -> new ShopMemberAccessDenied(
                        "ShopMemberAccessDenied",
                        "Tai khoan khong thuoc shop nay"
                ));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new ShopMemberAccessDenied(
                    "ShopMemberAccessDenied",
                    "Yeu cau nguoi dung da dang nhap"
            );
        }
        return userPrincipal.getUser().getId();
    }

    private ShopMember getMemberEntity(Long shopId, Long userId) {
        return shopMemberRepository.findByShopIdAndUserId(shopId, userId)
                .orElseThrow(() -> new ShopMemberNotFound(
                        "ShopMemberNotFound",
                        "Khong tim thay tai khoan trong shop"
                ));
    }

    private String normalizeRequiredValue(String value) {
        return value.trim();
    }

    private String normalizeOptionalValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
