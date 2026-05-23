package com.exe101.auth.service;

import com.exe101.auth.dto.AuthenticatedShopDTO;
import com.exe101.auth.dto.AuthenticationRequest;
import com.exe101.auth.dto.AuthenticationResponse;
import com.exe101.auth.dto.ForgotPasswordRequest;
import com.exe101.auth.dto.ForgotPasswordResponse;
import com.exe101.auth.dto.RegisterRequest;
import com.exe101.auth.dto.ResetPasswordRequest;
import com.exe101.auth.dto.VerifyOtpForgotPasswordRequest;
import com.exe101.auth.exception.AuthAccessDeniedException;
import com.exe101.auth.exception.LoginException;
import com.exe101.auth.model.RefreshToken;
import com.exe101.auth.model.UserPrincipal;
import com.exe101.email.entity.EmailVerificationPurpose;
import com.exe101.email.entity.EmailVerificationToken;
import com.exe101.email.exception.EmailValidationException;
import com.exe101.email.service.EmailService;
import com.exe101.file.FileUploadUtil;
import com.exe101.shop.entity.ShopStatus;
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
import java.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final EmailService emailService;

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

    public AuthenticationResponse authenticateShopOrAdmin(AuthenticationRequest request) {
        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            User user = principal.getUser();

            if (user.getRole() == UserRole.ADMIN) {
                validateActiveUser(user);
            } else {
                validateAuthenticatedUser(
                        user,
                        UserRole.SHOP,
                        true,
                        "ShopPortalOnly",
                        "Tai khoan nay khong the dang nhap vao trang quan ly shop"
                );
            }

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
            throw new LoginException("WrongPassOrEmail", "Email hoac mat khau khong dung");
        }
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
            User user = userRepository.findByEmailIgnoreCase(request.getEmail()).orElse(null);
            if (user != null) {
                UserCredential cred = credentialRepository.findById(user.getId()).orElse(null);
                if (cred != null && cred.getProvider() != CredentialProvider.LOCAL && (cred.getPasswordHash() == null || cred.getPasswordHash().isEmpty())) {
                    throw new LoginException("WrongProvider", "Tài khoản chưa thiết lập mật khẩu. Vui lòng đăng nhập bằng " + cred.getProvider());
                }
            }
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

        validateActiveUser(user);

        if (requireActiveShopMembership) {
            validateActiveApprovedShopMembership(user);
        }
    }

    private void validateActiveUser(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthAccessDeniedException(
                    "AccountInactive",
                    "Tai khoan nay khong con hoat dong"
            );
        }
    }

    private void validateActiveApprovedShopMembership(User user) {
        if (shopMemberRepository.existsByUserIdAndMemberStatusAndShopStatus(
                user.getId(),
                MemberStatus.ACTIVE,
                ShopStatus.ACTIVE
        )) {
            return;
        }

        if (shopMemberRepository.existsByUserIdAndShopStatus(
                user.getId(),
                ShopStatus.PENDING_APPROVAL
        )) {
            throw new AuthAccessDeniedException(
                    "ShopPendingApproval",
                    "Shop cua ban dang cho admin duyet"
            );
        }

        if (shopMemberRepository.existsByUserIdAndShopStatus(
                user.getId(),
                ShopStatus.REJECTED
        )) {
            throw new AuthAccessDeniedException(
                    "ShopRegistrationRejected",
                    "Dang ky shop cua ban da bi tu choi"
            );
        }

        throw new AuthAccessDeniedException(
                "ShopMembershipInactive",
                "Tai khoan shop nay chua co lien ket shop dang hoat dong"
        );
    }

    public AuthenticationResponse refreshToken(String refreshToken) {

        var rotated = refreshTokenService.rotate(refreshToken);

        User user = userRepository
                .findById(rotated.getUserId())
                .orElseThrow(() -> new UserNotFound("UserNotFound", "Không tìm thấy người dùng"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            refreshTokenService.revokeByToken(rotated.getToken());
            throw new AuthAccessDeniedException(
                    "AccountInactive",
                    "Tai khoan nay khong con hoat dong"
            );
        }

        if (user.getRole() == UserRole.SHOP) {
            try {
                validateActiveApprovedShopMembership(user);
            } catch (AuthAccessDeniedException ex) {
                refreshTokenService.revokeByToken(rotated.getToken());
                throw ex;
            }
        }

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
        return shopMemberRepository.findAuthenticatedShopsByUserIdAndStatus(
                user.getId(),
                MemberStatus.ACTIVE,
                ShopStatus.ACTIVE
        );
    }

    private Long resolveCurrentShopId(List<AuthenticatedShopDTO> shops) {
        return shops.isEmpty() ? null : shops.get(0).getId();
    }

    public void logout(String refreshToken) {
        refreshTokenService.revokeByToken(refreshToken);
    }

    // ── Forgot Password Flow ────────────────────────────────────────────

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        
        // Kiểm tra user tồn tại với email này và là CUSTOMER
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UserNotFound("UserNotFound", "Không tìm thấy người dùng với email này"));
        
        if (user.getRole() != UserRole.CUSTOMER) {
            throw new LoginException("InvalidRole", "Chỉ khách hàng mới có thể yêu cầu đặt lại mật khẩu");
        }

        // Tạo token xác nhận
        EmailVerificationToken token = emailService.createAndSendVerificationCode(
                user.getId(),
                normalizedEmail,
                EmailVerificationPurpose.RESET_PASSWORD
        );

        return new ForgotPasswordResponse(
                "Mã OTP đã được gửi đến email của bạn",
                normalizedEmail,
                calculateExpiresInSeconds(token.getExpiresAt())
        );
    }

    @Transactional
    public void verifyOtpForgotPassword(VerifyOtpForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        String otp = request.getOtp().trim();

        // Tìm token hợp lệ
        EmailVerificationToken token = emailService.findValidToken(
                normalizedEmail,
                otp,
                EmailVerificationPurpose.RESET_PASSWORD
        );

        if (token == null || token.getVerified()) {
            throw new EmailValidationException("InvalidOtp", "Mã OTP không hợp lệ hoặc đã hết hạn");
        }

        // Đánh dấu token là đã xác nhận (nhưng chưa sử dụng)
        token.setVerified(true);
        emailService.saveToken(token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        String otp = request.getOtp().trim();
        String newPassword = request.getNewPassword();

        // Lấy user
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UserNotFound("UserNotFound", "Không tìm thấy người dùng"));

        // Kiểm tra token đã được xác nhận
        EmailVerificationToken token = emailService.findValidToken(
                normalizedEmail,
                otp,
                EmailVerificationPurpose.RESET_PASSWORD
        );

        if (token == null || !token.getVerified()) {
            throw new EmailValidationException("UnverifiedOtp", "Mã OTP chưa được xác nhận hoặc đã hết hạn");
        }

        // Cập nhật mật khẩu
        UserCredential credential = credentialRepository.findById(user.getId())
                .orElseThrow(() -> new UserNotFound("UserNotFound", "Không tìm thấy thông tin đăng nhập"));

        credential.setPasswordHash(passwordEncoder.encode(newPassword));
        credential.setUpdatedAt(OffsetDateTime.now());
        credentialRepository.save(credential);

        // Đánh dấu token đã sử dụng
        token.setUsedAt(OffsetDateTime.now());
        emailService.saveToken(token);

        // Gửi email xác nhận thay đổi mật khẩu
        emailService.sendResetPasswordConfirmationEmail(user.getEmail(), user.getFullName());
    }

    private long calculateExpiresInSeconds(OffsetDateTime expiresAt) {
        return java.time.Duration.between(OffsetDateTime.now(), expiresAt).getSeconds();
    }

    @Transactional
    public AuthenticationResponse googleLogin(com.exe101.auth.dto.GoogleLoginRequest request) {
        try {
            // Decode Google ID Token (NOT verifying signature for now - add verification at production)
            String[] tokenParts = request.getIdToken().split("\\.");
            if (tokenParts.length != 3) {
                throw new LoginException("InvalidToken", "Token không hợp lệ");
            }

            // Decode payload (part 1)
            String payload = tokenParts[1];
            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload));
            ObjectMapper mapper = new ObjectMapper();
            java.util.Map<String, Object> claims;
            try {
                claims = mapper.readValue(decodedPayload, java.util.Map.class);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new LoginException("InvalidToken", "Không thể parse token: " + e.getMessage());
            }

            String email = (String) claims.get("email");
            String name = (String) claims.get("name");
            String pictureUrl = (String) claims.get("picture");
            String googleUserId = (String) claims.get("sub");

            if (email == null || email.isEmpty()) {
                throw new LoginException("InvalidGoogleToken", "Google token không chứa email");
            }

            // Find user by email or by Google provider ID
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElse(null);

            // If user doesn't exist, create new user
            if (user == null) {
                user = new User();
                user.setEmail(email.toLowerCase().trim());
                user.setFullName(name != null ? name : email.split("@")[0]);
                user.setPhone(""); // Will be updated later
                user.setAddress("");
                user.setAge(0);
                user.setRole(UserRole.CUSTOMER);
                user.setStatus(UserStatus.ACTIVE);
                user.setAvatarUrlPreview(pictureUrl);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                user = userRepository.save(user);

                createDefaultUserAddressForGoogle(user);
            }

            // Check or create credential with GOOGLE provider
            UserCredential credential = credentialRepository.findById(user.getId())
                    .orElse(new UserCredential());

            credential.setUser(user);
            credential.setProvider(CredentialProvider.GOOGLE);
            credential.setProviderUserId(googleUserId);
            credential.setCreatedAt(OffsetDateTime.now());
            credential.setUpdatedAt(OffsetDateTime.now());
            credentialRepository.save(credential);

            // Generate JWT response
            UserPrincipal principal = new UserPrincipal(user, credential);
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

        } catch (Exception ex) {
            if (ex instanceof LoginException) {
                throw ex;
            }
            throw new LoginException("GoogleLoginFailed", "Đăng nhập Google thất bại: " + ex.getMessage());
        }
    }

    private void createDefaultUserAddressForGoogle(User user) {
        UserAddress address = new UserAddress();
        address.setUserId(user.getId());
        address.setName("Địa chỉ mặc định");
        address.setTel("0000000000"); // Dummy phone to pass not-blank checks
        address.setProvince("Chưa cập nhật");
        address.setDistrict("Chưa cập nhật");
        address.setWard("Chưa cập nhật");
        address.setHamlet("Chưa cập nhật");
        address.setAddress("Chưa cập nhật");
        address.setDefaultAddress(true);
        userAddressRepository.save(address);
    }

    @Transactional
    public AuthenticationResponse facebookLogin(com.exe101.auth.dto.FacebookLoginRequest request) {
        try {
            // Call Facebook Graph API to verify token and get user info
            String graphUrl = "https://graph.facebook.com/me?fields=id,name,email,picture&access_token=" + request.getAccessToken();
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            java.util.Map<String, Object> response;
            try {
                response = restTemplate.getForObject(graphUrl, java.util.Map.class);
            } catch (Exception e) {
                throw new LoginException("InvalidFacebookToken", "Token Facebook không hợp lệ hoặc đã hết hạn");
            }
            
            if (response == null || !response.containsKey("id")) {
                throw new LoginException("InvalidFacebookToken", "Không thể lấy thông tin từ Facebook");
            }

            String facebookUserId = (String) response.get("id");
            String name = (String) response.get("name");
            String email = (String) response.get("email");
            
            // Get avatar URL
            String pictureUrl = null;
            if (response.containsKey("picture")) {
                java.util.Map<String, Object> picture = (java.util.Map<String, Object>) response.get("picture");
                if (picture != null && picture.containsKey("data")) {
                    java.util.Map<String, Object> data = (java.util.Map<String, Object>) picture.get("data");
                    if (data != null && data.containsKey("url")) {
                        pictureUrl = (String) data.get("url");
                    }
                }
            }

            if (email == null || email.isEmpty()) {
                // If user registered Facebook with phone number, email might be missing
                email = facebookUserId + "@facebook.com"; // Fallback email
            }

            // Find user by email
            User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

            // If user doesn't exist, create new user
            if (user == null) {
                user = new User();
                user.setEmail(email.toLowerCase().trim());
                user.setFullName(name != null ? name : email.split("@")[0]);
                user.setPhone(""); 
                user.setAddress("");
                user.setAge(0);
                user.setRole(UserRole.CUSTOMER);
                user.setStatus(UserStatus.ACTIVE);
                user.setAvatarUrlPreview(pictureUrl);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                user = userRepository.save(user);

                createDefaultUserAddressForGoogle(user);
            }

            // Check or create credential with FACEBOOK provider
            UserCredential credential = credentialRepository.findById(user.getId())
                    .orElse(new UserCredential());

            credential.setUser(user);
            credential.setProvider(CredentialProvider.FACEBOOK);
            credential.setProviderUserId(facebookUserId);
            credential.setCreatedAt(OffsetDateTime.now());
            credential.setUpdatedAt(OffsetDateTime.now());
            credentialRepository.save(credential);

            // Generate JWT response
            UserPrincipal principal = new UserPrincipal(user, credential);
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

        } catch (Exception ex) {
            if (ex instanceof LoginException) {
                throw ex;
            }
            throw new LoginException("FacebookLoginFailed", "Đăng nhập Facebook thất bại: " + ex.getMessage());
        }
    }
}
