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

    private static final List<ShopRole> MEMBER_MANAGE_ROLES = List.of(ShopRole.OWNER, ShopRole.MANAGER);

    private final IShopMemberRepository shopMemberRepository;
    private final IUserRepository userRepository;
    private final IUserCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;

    public List<ShopMemberDTO> getActiveStaffByShop(Long shopId) {
        assertCanViewMembers(shopId);
        return shopMemberRepository.findByShopIdAndRoleAndStatusForDisplay(
                shopId,
                ShopRole.STAFF,
                MemberStatus.ACTIVE
        );
    }

    public List<ShopMemberDTO> getAllByShop(Long shopId, ShopRole role, MemberStatus status, String keyword) {
        assertCanViewMembers(shopId);
        if (role == ShopRole.OWNER) {
            return List.of();
        }
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return shopMemberRepository.findByShopIdForDisplay(
                shopId,
                ShopRole.OWNER,
                role,
                status,
                normalizedKeyword
        );
    }

    public ShopMemberDTO getByShopAndUserId(Long shopId, Long userId) {
        assertCanViewMembers(shopId);
        return shopMemberRepository.findDetailByShopIdAndUserId(shopId, userId)
                .orElseThrow(() -> new ShopMemberNotFound("ShopMemberNotFound", "Không tìm thấy thành viên trong shop"));
    }

    @Transactional
    public ShopMemberDTO create(Long shopId, ShopMemberCreateRequest request) {
        ShopMember actorMembership = assertCanManageMembers(shopId);
        assertCanAssignRole(actorMembership.getRole(), request.getRole());
        validateUniqueUser(request.getEmail(), request.getPhone());

        User user = createShopUser(request);
        createCredential(user, request.getPassword());

        MemberStatus targetStatus = request.getStatus() != null ? request.getStatus() : MemberStatus.ACTIVE;
        ShopMember member = new ShopMember();
        member.setId(new ShopMemberId(shopId, user.getId()));
        member.setRole(request.getRole());
        member.setStatus(targetStatus);

        shopMemberRepository.save(member);
        return getByShopAndUserId(shopId, user.getId());
    }

    @Transactional
    public ShopMemberDTO update(Long shopId, Long userId, ShopMemberUpdateRequest request) {
        if (request.getRole() == null && request.getStatus() == null) {
            throw new ShopMemberValidationException(
                    "ShopMemberUpdateEmpty",
                    "Cần cung cấp ít nhất vai trò hoặc trạng thái để cập nhật"
            );
        }

        ShopMember actorMembership = assertCanManageMembers(shopId);
        ShopMember member = getMemberEntity(shopId, userId);
        Long currentUserId = actorMembership.getUserId();

        if (currentUserId.equals(userId)) {
            throw new ShopMemberValidationException(
                    "ShopMemberSelfUpdateNotAllowed",
                    "Bạn không thể tự thay đổi vai trò hoặc trạng thái của chính mình"
            );
        }

        ShopRole nextRole = request.getRole() != null ? request.getRole() : member.getRole();
        MemberStatus nextStatus = request.getStatus() != null ? request.getStatus() : member.getStatus();

        assertCanModifyTarget(actorMembership.getRole(), member, nextRole);
        assertOwnerStillExistsAfterChange(shopId, member, nextRole, nextStatus);

        member.setRole(nextRole);
        member.setStatus(nextStatus);
        shopMemberRepository.save(member);
        return getByShopAndUserId(shopId, userId);
    }

    @Transactional
    public void resetPassword(Long shopId, Long userId, ShopMemberResetPasswordRequest request) {
        ShopMember actorMembership = assertCanManageMembers(shopId);
        ShopMember member = getMemberEntity(shopId, userId);

        if (actorMembership.getUserId().equals(userId)) {
            throw new ShopMemberValidationException(
                    "ShopMemberSelfResetPasswordNotAllowed",
                    "Bạn không thể dùng API này để tự đặt lại mật khẩu của chính mình"
            );
        }

        assertCanModifyTarget(actorMembership.getRole(), member, member.getRole());

        UserCredential credential = credentialRepository.findById(userId)
                .orElseThrow(() -> new ShopMemberNotFound(
                        "ShopMemberCredentialNotFound",
                        "Không tìm thấy thông tin đăng nhập của thành viên"
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
                    "Bạn không thể tự xóa chính mình khỏi shop"
            );
        }

        assertCanModifyTarget(actorMembership.getRole(), member, member.getRole());
        assertOwnerStillExistsAfterChange(shopId, member, member.getRole(), MemberStatus.REMOVED);

        member.setStatus(MemberStatus.REMOVED);
        shopMemberRepository.save(member);
    }

    private void validateUniqueUser(String email, String phone) {
        if (userRepository.existsByEmail(normalizeRequiredValue(email))) {
            throw new UserDuplicate("EmailUserDuplicate", "Email đã tồn tại");
        }
        if (userRepository.existsByPhone(normalizeRequiredValue(phone))) {
            throw new UserDuplicate("PhoneUserDuplicate", "Số điện thoại đã tồn tại");
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
                    "Bạn không có quyền xem danh sách thành viên của shop này"
            );
        }
    }

    private ShopMember assertCanManageMembers(Long shopId) {
        ShopMember membership = getCurrentMembership(shopId);
        if (membership.getStatus() != MemberStatus.ACTIVE || !MEMBER_MANAGE_ROLES.contains(membership.getRole())) {
            throw new ShopMemberAccessDenied(
                    "ShopMemberAccessDenied",
                    "Chỉ chủ shop hoặc quản lý mới được quản lý thành viên"
            );
        }
        return membership;
    }

    private ShopMember getCurrentMembership(Long shopId) {
        Long currentUserId = getCurrentUserId();
        return shopMemberRepository.findByShopIdAndUserId(shopId, currentUserId)
                .orElseThrow(() -> new ShopMemberAccessDenied(
                        "ShopMemberAccessDenied",
                        "Bạn không thuộc shop này"
                ));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new ShopMemberAccessDenied(
                    "ShopMemberAccessDenied",
                    "Yêu cầu người dùng đã đăng nhập"
            );
        }
        return userPrincipal.getUser().getId();
    }

    private ShopMember getMemberEntity(Long shopId, Long userId) {
        return shopMemberRepository.findByShopIdAndUserId(shopId, userId)
                .orElseThrow(() -> new ShopMemberNotFound("ShopMemberNotFound", "Không tìm thấy thành viên trong shop"));
    }

    private void assertCanAssignRole(ShopRole actorRole, ShopRole targetRole) {
        if (actorRole == ShopRole.MANAGER && targetRole == ShopRole.OWNER) {
            throw new ShopMemberAccessDenied(
                    "ShopMemberAssignOwnerDenied",
                    "Quản lý không thể thêm hoặc chuyển thành chủ shop"
            );
        }
    }

    private void assertCanModifyTarget(ShopRole actorRole, ShopMember targetMember, ShopRole nextRole) {
        if (actorRole == ShopRole.MANAGER && (targetMember.getRole() == ShopRole.OWNER || nextRole == ShopRole.OWNER)) {
            throw new ShopMemberAccessDenied(
                    "ShopMemberModifyOwnerDenied",
                    "Quản lý không thể chỉnh sửa thành viên có vai trò chủ shop"
            );
        }
    }

    private void assertOwnerStillExistsAfterChange(
            Long shopId,
            ShopMember targetMember,
            ShopRole nextRole,
            MemberStatus nextStatus
    ) {
        boolean removingActiveOwner = targetMember.getRole() == ShopRole.OWNER
                && targetMember.getStatus() == MemberStatus.ACTIVE
                && (nextRole != ShopRole.OWNER || nextStatus != MemberStatus.ACTIVE);

        if (!removingActiveOwner) {
            return;
        }

        long activeOwnerCount = shopMemberRepository.countByShopIdAndRoleAndStatus(shopId, ShopRole.OWNER, MemberStatus.ACTIVE);
        if (activeOwnerCount <= 1) {
            throw new ShopMemberValidationException(
                    "ShopMemberLastOwnerInvalid",
                    "Shop phải luôn có ít nhất một chủ shop đang hoạt động"
            );
        }
    }

    private String normalizeRequiredValue(String value) {
        return value.trim();
    }

    private String normalizeOptionalValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
