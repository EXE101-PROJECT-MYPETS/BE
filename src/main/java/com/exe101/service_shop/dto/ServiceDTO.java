package com.exe101.service_shop.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDTO {

    private Long id;

    private Long shopId;

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

    private Boolean active;
}
