package com.exe101.email.dto;

import com.exe101.email.entity.EmailVerificationPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerificationVerifyRequest {

    @Email(message = "Email không đúng định dạng")
    @NotBlank(message = "Email không được để trống")
    private String email;

    @Pattern(regexp = "^\\d{6}$", message = "Mã xác thực phải gồm 6 chữ số")
    @NotBlank(message = "Mã xác thực không được để trống")
    private String code;

    @NotNull(message = "Mục đích xác thực không được để trống")
    private EmailVerificationPurpose purpose;
}
