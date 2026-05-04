package com.exe101.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryDTO {

    private Long id;

    private Long shopId;

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name max 100 chars")
    private String name;

    private String description;

    private Boolean active;

    @Min(value = 0, message = "sortOrder must be >= 0")
    private Integer sortOrder;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
