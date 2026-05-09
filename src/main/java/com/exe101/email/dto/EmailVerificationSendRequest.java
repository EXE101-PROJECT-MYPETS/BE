package com.exe101.email.dto;

import com.exe101.email.entity.EmailVerificationPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerificationSendRequest {

    private Long userId;

    @Email(message = "Email không đúng định dạng")
    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotNull(message = "Mục đích xác thực không được để trống")
    private EmailVerificationPurpose purpose;
}
