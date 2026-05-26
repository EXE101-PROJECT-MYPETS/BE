package com.exe101.user.dto;

import com.exe101.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private Integer age;
    private String avatarUrlPreview;
    private UserStatus status;
}
