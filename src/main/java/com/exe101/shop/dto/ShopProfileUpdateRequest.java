package com.exe101.shop.dto;

import com.exe101.shop.entity.LocationSource;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ShopProfileUpdateRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 500)
    private String addressText;

    @Size(max = 1000)
    private String imageUrl;

    private MultipartFile avatar;

    @Size(max = 1000)
    private String coverImageUrl;

    private MultipartFile cover_img;

    @Size(max = 50)
    private String phone;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 2000)
    private String description;

    @Size(max = 20)
    private String openingHours;

    @Size(max = 20)
    private String closingHours;

    @Size(max = 1000)
    private String facebookUrl;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double lat;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double lng;

    @NotNull
    private LocationSource locationSource;

    @PositiveOrZero
    private Integer locationAccuracyM;
}
