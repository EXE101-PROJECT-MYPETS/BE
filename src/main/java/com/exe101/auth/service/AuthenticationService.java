package com.exe101.auth.service;

import com.exe101.auth.dto.AuthenticatedShopDTO;
import com.exe101.auth.dto.AuthenticationRequest;
import com.exe101.auth.dto.AuthenticationResponse;
import com.exe101.auth.dto.RegisterRequest;
import com.exe101.auth.exception.AuthAccessDeniedException;
import com.exe101.auth.exception.LoginException;
import com.exe101.auth.model.RefreshToken;
import com.exe101.auth.model.UserPrincipal;
import com.exe101.file.FileUploadUtil;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import com.exe101.user.entity.User;
import com.exe101.user.entity.UserRole;
import com.exe101.user.entity.UserStatus;
import com.exe101.user.exception.UserDuplicate;
import com.exe101.user.exception.UserNotFound;
import com.exe101.user.mapper.UserMapper;
import com.exe101.user.repository.IUserRepository;
import com.exe101.userAddress.entity.UserAddress;
import com.exe101.userAddress.repository.IUserAddressRepository;
import com.exe101.userCredential.entity.CredentialProvider;
import com.exe101.userCredential.entity.UserCredential;
import com.exe101.userCredential.repository.IUserCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final IUserCredentialRepository credentialRepository;
    private final UserMapper userMapper;
    private final FileUploadUtil fileUploadUtil;
    private final IShopMemberRepository shopMemberRepository;
    private final IUserAddressRepository userAddressRepository;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserDuplicate("EmailUserDuplicate", "Email đã tồn tại");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new UserDuplicate("PhoneUserDuplicate", "Số điện thoại đã tồn tại");
        }

        User user = new User();
        user.setEmail(normalizeRequiredValue(request.getEmail()));
        user.setFullName(normalizeRequiredValue(request.getFullName()));
        user.setPhone(normalizeRequiredValue(request.getPhone()));
        user.setAddress(toDisplayAddress(request));
        user.setAge(request.getAge());
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        user = userRepository.save(user);
        createDefaultUserAddress(user, request);

        MultipartFile avatarFile = request.getAvatarUrlPreview();
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String avatarUrl = fileUploadUtil.uploadUserAvatar(user.getId(), avatarFile);
            user.setAvatarUrlPreview(avatarUrl);
            user.setUpdatedAt(LocalDateTime.now());
            user = userRepository.save(user);
        }

        UserCredential cred = new UserCredential();
        cred.setProvider(CredentialProvider.LOCAL);
        cred.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        cred.setUser(user);
        cred.setCreatedAt(OffsetDateTime.now());
        cred.setUpdatedAt(OffsetDateTime.now());
        cred.setProviderUserId(null);
        credentialRepository.save(cred);

        UserPrincipal principal = new UserPrincipal(user, cred);
        String accessToken = jwtService.generateToken(principal);
        RefreshToken refreshToken = refreshTokenService.create(user.getId());

        return new AuthenticationResponse(
                accessToken,
                user.getRole(),
                refreshToken.getToken(),
                userMapper.toDTO(user)
        );
    }

    public AuthenticationResponse authenticateCustomer(AuthenticationRequest request) {
        return authenticateForRole(
                request,
                UserRole.CUSTOMER,
                false,
                "CustomerPortalOnly",
                "Tài khoản này không thể đăng nhập vào ứng dụng khách hàng"
        );
    }

    public AuthenticationResponse authenticateShop(AuthenticationRequest request) {
        return authenticateForRole(
                request,
                UserRole.SHOP,
                true,
                "ShopPortalOnly",
                "Tài khoản này không thể đăng nhập vào trang quản lý shop"
        );
    }

    private AuthenticationResponse authenticateForRole(
            AuthenticationRequest request,
            UserRole expectedRole,
            boolean requireActiveShopMembership,
            String accessDeniedCode,
            String accessDeniedMessage
    ) {
        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            User user = principal.getUser();
            validateAuthenticatedUser(
                    user,
                    expectedRole,
                    requireActiveShopMembership,
                    accessDeniedCode,
                    accessDeniedMessage
            );

            String accessToken = jwtService.generateToken(principal);
            RefreshToken refreshToken = refreshTokenService.create(user.getId());
            List<AuthenticatedShopDTO> shops = resolveAuthenticatedShops(user);

            return new AuthenticationResponse(
                    accessToken,
                    user.getRole(),
                    refreshToken.getToken(),
                    userMapper.toDTO(user),
                    shops,
                    resolveCurrentShopId(shops)

            );

        } catch (BadCredentialsException ex) {
            throw new LoginException("WrongPassOrEmail", "Email hoặc mật khẩu không đúng");
        }
    }

    private void validateAuthenticatedUser(
            User user,
            UserRole expectedRole,
            boolean requireActiveShopMembership,
            String accessDeniedCode,
            String accessDeniedMessage
    ) {
        if (user.getRole() != expectedRole) {
            throw new AuthAccessDeniedException(accessDeniedCode, accessDeniedMessage);
        }

        if (requireActiveShopMembership
                && !shopMemberRepository.existsByUserIdAndStatus(user.getId(), MemberStatus.ACTIVE)) {
            throw new AuthAccessDeniedException(
                    "ShopMembershipInactive",
                    "Tài khoản shop này chưa có liên kết shop đang hoạt động"
            );
        }
    }

    public AuthenticationResponse refreshToken(String refreshToken) {

        var rotated = refreshTokenService.rotate(refreshToken);

        User user = userRepository
                .findById(rotated.getUserId())
                .orElseThrow(() -> new UserNotFound("UserNotFound", "Không tìm thấy người dùng"));

        var cred = credentialRepository
                .findById(user.getId())
                .orElseThrow(() -> new UserNotFound("UserNotFound", "Không tìm thấy người dùng"));

        var principal = new UserPrincipal(user, cred);

        String accessToken = jwtService.generateToken(principal);
        List<AuthenticatedShopDTO> shops = resolveAuthenticatedShops(user);

        return new AuthenticationResponse(
                accessToken,
                user.getRole(),
                rotated.getToken(),
                userMapper.toDTO(user),
                shops,
                resolveCurrentShopId(shops)
        );
    }

    private void createDefaultUserAddress(User user, RegisterRequest request) {
        UserAddress address = new UserAddress();
        address.setUserId(user.getId());
        address.setName(normalizeRequiredValue(request.getFullName()));
        address.setTel(normalizeRequiredValue(request.getPhone()));
        address.setAddress(normalizeRequiredValue(request.getAddress()));
        address.setProvince(normalizeRequiredValue(request.getProvince()));
        address.setDistrict(normalizeRequiredValue(request.getDistrict()));
        address.setWard(normalizeRequiredValue(request.getWard()));
        address.setHamlet(normalizeRequiredValue(request.getHamlet()));
        address.setDefaultAddress(true);
        userAddressRepository.save(address);
    }

    private String toDisplayAddress(RegisterRequest request) {
        return String.join(
                ", ",
                normalizeRequiredValue(request.getAddress()),
                normalizeRequiredValue(request.getHamlet()),
                normalizeRequiredValue(request.getWard()),
                normalizeRequiredValue(request.getDistrict()),
                normalizeRequiredValue(request.getProvince())
        );
    }

    private String normalizeRequiredValue(String value) {
        return value.trim();
    }

    private List<AuthenticatedShopDTO> resolveAuthenticatedShops(User user) {
        if (user.getRole() != UserRole.SHOP) {
            return List.of();
        }
        return shopMemberRepository.findAuthenticatedShopsByUserIdAndStatus(user.getId(), MemberStatus.ACTIVE);
    }

    private Long resolveCurrentShopId(List<AuthenticatedShopDTO> shops) {
        return shops.isEmpty() ? null : shops.get(0).getId();
    }

    public void logout(String refreshToken) {
        refreshTokenService.revokeByToken(refreshToken);
    }
}
