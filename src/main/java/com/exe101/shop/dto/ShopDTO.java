package com.exe101.shop.dto;

import com.exe101.shop.entity.LocationSource;
import com.exe101.shop.entity.ShopStatus;
import com.exe101.user.dto.UserDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class ShopDTO {

    private Long id;

    @NotBlank(message = "Tên shop không được để trống")
    @Size(max = 255, message = "Tên shop không được vượt quá 255 ký tự")
    private String name;

    @Size(max = 500, message = "Địa chỉ shop không được vượt quá 500 ký tự")
    private String addressText;

    @Size(max = 1000, message = "Ảnh đại diện shop không được vượt quá 1000 ký tự")
    private String imageUrl;

    @Size(max = 1000)
    private String coverImageUrl;

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

    @NotNull(message = "Vĩ độ không được để trống")
    @DecimalMin(value = "-90.0", message = "Vĩ độ phải lớn hơn hoặc bằng -90")
    @DecimalMax(value = "90.0", message = "Vĩ độ phải nhỏ hơn hoặc bằng 90")
    private Double lat;

    @NotNull(message = "Kinh độ không được để trống")
    @DecimalMin(value = "-180.0", message = "Kinh độ phải lớn hơn hoặc bằng -180")
    @DecimalMax(value = "180.0", message = "Kinh độ phải nhỏ hơn hoặc bằng 180")
    private Double lng;

    @NotNull(message = "Nguồn vị trí không được để trống")
    private LocationSource locationSource;

    @PositiveOrZero(message = "Độ chính xác vị trí phải lớn hơn hoặc bằng 0")
    private Integer locationAccuracyM;

    private ShopStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UserDTO owner;
}
