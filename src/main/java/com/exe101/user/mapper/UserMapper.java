package com.exe101.user.mapper;

import com.exe101.user.dto.UserDTO;
import com.exe101.user.dto.UserProfileDTO;
import com.exe101.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAddress(),
                user.getAge(),
                user.getAvatarUrlPreview(),
                user.getRole()
        );
    }

    public User toEntity(UserDTO dto) {
        if (dto == null) return null;
        User user = new User();
        user.setId(dto.getId());
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setRole(dto.getRole());
        return user;
    }

    public UserProfileDTO toProfileDTO(User user) {
        return new UserProfileDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAge(),
                user.getAvatarUrlPreview(),
                user.getStatus()
        );
    }
}
