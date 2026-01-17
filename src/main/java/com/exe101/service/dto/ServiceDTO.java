package com.exe101.service.dto;


import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDTO {

    private Long id;

    @NotNull(message = "shopId is required for create")
    private Long shopId;

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name max 255 chars")
    private String name;

    @NotNull(message = "durationMin is required for create")
    @Min(value = 1, message = "durationMin must be > 0")
    private Integer durationMin;

    @NotNull(message = "basePrice is required for create")
    @Min(value = 0, message = "basePrice must be >= 0")
    private Long basePrice;

    private Boolean active;
}
