package com.exe101.service_shop.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ServiceCreateRequest {

    @NotBlank(message = "Tên dịch vụ không được để trống")
    @Size(max = 255, message = "Tên dịch vụ không được vượt quá 255 ký tự")
    private String name;

    @NotNull(message = "Thời lượng dịch vụ không được để trống")
    @Min(value = 1, message = "Thời lượng dịch vụ phải lớn hơn 0")
    private Integer durationMin;

    @NotNull(message = "Giá dịch vụ không được để trống")
    @Min(value = 0, message = "Giá dịch vụ phải lớn hơn hoặc bằng 0")
    private Long basePrice;

    private Long categoryId;

    @Size(max = 1000, message = "Đường dẫn ảnh không được vượt quá 1000 ký tự")
    private String imageUrl;

    private Boolean active;

    private MultipartFile imageUrlPreview;
}
