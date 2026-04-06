package com.exe101.auth.service;

import com.exe101.auth.dto.AuthenticationRequest;
import com.exe101.auth.dto.AuthenticationResponse;
import com.exe101.auth.dto.RegisterRequest;
import com.exe101.auth.exception.LoginException;
import com.exe101.auth.model.RefreshToken;
import com.exe101.auth.model.UserPrincipal;
import com.exe101.file.FileUploadUtil;
import com.exe101.user.entity.User;
import com.exe101.user.entity.UserRole;
import com.exe101.user.entity.UserStatus;
import com.exe101.user.exception.UserDuplicate;
import com.exe101.user.exception.UserNotFound;
import com.exe101.user.mapper.UserMapper;
import com.exe101.user.repository.IUserRepository;
import com.exe101.userCredential.entity.CredentialProvider;
import com.exe101.userCredential.entity.UserCredential;
import com.exe101.userCredential.repository.IUserCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

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

    public AuthenticationResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserDuplicate("EmailUserDuplicate", "Email đã tồn tại!");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new UserDuplicate("PhoneUserDuplicate", "Số điện thoại đã tồn tại!");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setAge(request.getAge());
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        user = userRepository.save(user);

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

    public AuthenticationResponse authenticate(AuthenticationRequest request) {

        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            User user = principal.getUser();

            String accessToken = jwtService.generateToken(principal);
            RefreshToken refreshToken = refreshTokenService.create(user.getId());

            return new AuthenticationResponse(
                    accessToken,
                    user.getRole(),
                    refreshToken.getToken(),
                    userMapper.toDTO(user)

            );

        } catch (BadCredentialsException ex) {
            throw new LoginException("WrongPassOrEmail", "Sai email hoặc mật khẩu");
        }
    }

    public AuthenticationResponse refreshToken(String refreshToken) {

        var rotated = refreshTokenService.rotate(refreshToken);

        User user = userRepository
                .findById(rotated.getUserId())
                .orElseThrow(() -> new UserNotFound("UserNotFound", "User not found"));

        var cred = credentialRepository
                .findById(user.getId())
                .orElseThrow(() -> new UserNotFound("UserNotFound", "User not found"));

        var principal = new UserPrincipal(user, cred);

        String accessToken = jwtService.generateToken(principal);

        return new AuthenticationResponse(
                accessToken,
                user.getRole(),
                rotated.getToken(),
                userMapper.toDTO(user)
        );
    }

    public void logout(String refreshToken) {
        refreshTokenService.revokeByToken(refreshToken);
    }
}
