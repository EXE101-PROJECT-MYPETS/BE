package com.exe101.auth.dto;

import com.exe101.shop.entity.LocationSource;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ShopOwnerRegisterRequest {

    @Email
    @NotBlank
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 6, max = 32)
    private String password;

    @NotBlank
    @Size(max = 255)
    private String fullName;

    @NotBlank
    @Size(max = 50)
    private String phone;

    @Size(max = 255)
    private String address;

    @Min(0)
    @Max(150)
    private Integer age;

    private MultipartFile avatarUrlPreview;

    @NotBlank
    @Size(max = 255)
    private String shopName;

    @NotBlank
    @Size(max = 500)
    private String shopAddressText;

    @NotNull
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double lat;

    @NotNull
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double lng;

    private LocationSource locationSource = LocationSource.MANUAL;

    @PositiveOrZero
    private Integer locationAccuracyM;
}
