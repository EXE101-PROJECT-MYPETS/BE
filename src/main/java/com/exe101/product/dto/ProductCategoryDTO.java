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

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự")
    private String name;

    private String description;

    private Boolean active;

    @Min(value = 0, message = "Thứ tự sắp xếp phải lớn hơn hoặc bằng 0")
    private Integer sortOrder;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
