package com.exe101.user.service;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.common.IService;
import com.exe101.file.FileUploadUtil;
import com.exe101.user.dto.UserDTO;
import com.exe101.user.dto.UserProfileUpdateRequest;
import com.exe101.user.entity.User;
import com.exe101.user.exception.UserDuplicate;
import com.exe101.user.exception.UserNotFound;
import com.exe101.user.exception.UserValidationException;
import com.exe101.user.mapper.UserMapper;
import com.exe101.user.repository.IUserRepository;
import com.exe101.userCredential.entity.CredentialProvider;
import com.exe101.userCredential.entity.UserCredential;
import com.exe101.userCredential.repository.IUserCredentialRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class UserService implements IService<User, UserDTO, Long> {

    private final IUserRepository userRepository;
    private final UserMapper userMapper;
    private final IUserCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadUtil fileUploadUtil;

    public UserService(
            IUserRepository userRepository,
            UserMapper userMapper,
            IUserCredentialRepository credentialRepository,
            PasswordEncoder passwordEncoder,
            FileUploadUtil fileUploadUtil
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileUploadUtil = fileUploadUtil;
    }

    @Override
    public List<UserDTO> getAll() {
        return List.of();
    }

    @Override
    public UserDTO getById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDTO)
                .orElseThrow(() -> new UserNotFound("UserNotFound", "Không tìm thấy người dùng"));
    }

    public UserDTO getCurrentUser(UserPrincipal principal) {
        return getById(getCurrentUserId(principal));
    }

    @Transactional
    public UserDTO updateCurrentUser(UserPrincipal principal, UserProfileUpdateRequest request) {
        Long currentUserId = getCurrentUserId(principal);
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFound("UserNotFound", "Không tìm thấy người dùng"));

        String email = normalizeRequiredValue(request.getEmail(), "Email không được để trống");
        String fullName = normalizeRequiredValue(request.getFullName(), "Họ và tên không được để trống");
        String phone = normalizeRequiredValue(request.getPhone(), "Số điện thoại không được để trống");

        if (!email.equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
            throw new UserDuplicate("EmailUserDuplicate", "Email đã tồn tại");
        }
        if (!phone.equals(user.getPhone())
                && userRepository.existsByPhoneAndIdNot(phone, user.getId())) {
            throw new UserDuplicate("PhoneUserDuplicate", "Số điện thoại đã tồn tại");
        }

        user.setEmail(email);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setAddress(normalizeOptionalValue(request.getAddress()));
        user.setAge(request.getAge());

        updatePasswordIfRequested(user.getId(), request);
        uploadAvatarIfPresent(user, request.getAvatarUrlPreview());

        user.setUpdatedAt(LocalDateTime.now());
        return userMapper.toDTO(userRepository.save(user));
    }

    @Override
    public UserDTO create(UserDTO dto) {
        return null;
    }

    @Override
    public UserDTO update(Long aLong, UserDTO dto) {
        return null;
    }

    @Override
    public void delete(Long aLong) {

    }

    private void updatePasswordIfRequested(Long userId, UserProfileUpdateRequest request) {
        boolean hasCurrentPassword = StringUtils.hasText(request.getCurrentPassword());
        boolean hasNewPassword = StringUtils.hasText(request.getNewPassword());

        if (!hasCurrentPassword && !hasNewPassword) {
            return;
        }
        if (!hasCurrentPassword || !hasNewPassword) {
            throw new UserValidationException(
                    "PasswordChangeInvalid",
                    "Cần nhập mật khẩu hiện tại và mật khẩu mới"
            );
        }

        UserCredential credential = credentialRepository.findById(userId)
                .orElseThrow(() -> new UserNotFound(
                        "UserCredentialNotFound",
                        "Không tìm thấy thông tin đăng nhập"
                ));

        if (credential.getProvider() != CredentialProvider.LOCAL
                || !StringUtils.hasText(credential.getPasswordHash())) {
            throw new UserValidationException(
                    "PasswordChangeProviderNotSupported",
                    "Tài khoản này không thể đổi mật khẩu bằng phương thức hiện tại"
            );
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), credential.getPasswordHash())) {
            throw new UserValidationException(
                    "CurrentPasswordInvalid",
                    "Mật khẩu hiện tại không đúng"
            );
        }

        credential.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        credential.setUpdatedAt(OffsetDateTime.now());
        credentialRepository.save(credential);
    }

    private void uploadAvatarIfPresent(User user, MultipartFile avatarFile) {
        if (avatarFile == null || avatarFile.isEmpty()) {
            return;
        }
        user.setAvatarUrlPreview(fileUploadUtil.uploadUserAvatar(user.getId(), avatarFile));
    }

    private Long getCurrentUserId(UserPrincipal principal) {
        if (principal == null || principal.getUser() == null || principal.getUser().getId() == null) {
            throw new UserValidationException(
                    "AuthenticatedUserRequired",
                    "Cần đăng nhập để cập nhật thông tin cá nhân"
            );
        }
        return principal.getUser().getId();
    }

    private String normalizeRequiredValue(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new UserValidationException("RequiredUserProfileField", message);
        }
        return value.trim();
    }

    private String normalizeOptionalValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
