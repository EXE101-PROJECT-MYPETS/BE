package com.exe101.auth.dto;

import com.exe101.shop.entity.LocationSource;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ShopOwnerRegisterRequest {

    @Email(message = "Email không đúng định dạng")
    @NotBlank(message = "Email không được để trống")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 32, message = "Mật khẩu phải có từ 6 đến 32 ký tự")
    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 255, message = "Họ và tên không được vượt quá 255 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 50, message = "Số điện thoại không được vượt quá 50 ký tự")
    private String phone;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    @Min(value = 0, message = "Tuổi phải lớn hơn hoặc bằng 0")
    @Max(value = 150, message = "Tuổi phải nhỏ hơn hoặc bằng 150")
    private Integer age;

    private MultipartFile avatarUrlPreview;

    @NotBlank(message = "Tên shop không được để trống")
    @Size(max = 255, message = "Tên shop không được vượt quá 255 ký tự")
    private String shopName;

    @NotBlank(message = "Địa chỉ shop không được để trống")
    @Size(max = 500, message = "Địa chỉ shop không được vượt quá 500 ký tự")
    private String shopAddressText;

    @NotNull(message = "Vĩ độ không được để trống")
    @DecimalMin(value = "-90.0", message = "Vĩ độ phải lớn hơn hoặc bằng -90")
    @DecimalMax(value = "90.0", message = "Vĩ độ phải nhỏ hơn hoặc bằng 90")
    private Double lat;

    @NotNull(message = "Kinh độ không được để trống")
    @DecimalMin(value = "-180.0", message = "Kinh độ phải lớn hơn hoặc bằng -180")
    @DecimalMax(value = "180.0", message = "Kinh độ phải nhỏ hơn hoặc bằng 180")
    private Double lng;

    private LocationSource locationSource = LocationSource.MANUAL;

    @PositiveOrZero(message = "Độ chính xác vị trí phải lớn hơn hoặc bằng 0")
    private Integer locationAccuracyM;
}
