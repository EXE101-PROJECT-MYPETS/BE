package com.exe101.shop.dto;

import com.exe101.shop.entity.LocationSource;
import com.exe101.shop.entity.ShopStatus;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;

@Getter
@Setter
public class ShopDTO {

    private Long id;

    @NotBlank(message = "Shop name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 500)
    private String addressText;

    @NotNull
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double lat;

    @NotNull
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double lng;

    @NotNull
    private LocationSource locationSource;

    @PositiveOrZero
    private Integer locationAccuracyM;

    private ShopStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
