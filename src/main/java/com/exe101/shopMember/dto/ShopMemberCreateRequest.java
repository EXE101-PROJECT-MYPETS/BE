package com.exe101.shopMember.dto;

import com.exe101.shop.entity.ShopRole;
import com.exe101.shopMember.entity.MemberStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopMemberCreateRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 255, message = "Họ và tên không được vượt quá 255 ký tự")
    private String fullName;

    @Email(message = "Email không đúng định dạng")
    @NotBlank(message = "Email không được để trống")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 32, message = "Mật khẩu phải có từ 6 đến 32 ký tự")
    private String password;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 50, message = "Số điện thoại không được vượt quá 50 ký tự")
    private String phone;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    @Min(value = 0, message = "Tuổi phải lớn hơn hoặc bằng 0")
    @Max(value = 150, message = "Tuổi phải nhỏ hơn hoặc bằng 150")
    private Integer age;

    @NotNull(message = "Vai trò không được để trống")
    private ShopRole role;

    private MemberStatus status;
}
