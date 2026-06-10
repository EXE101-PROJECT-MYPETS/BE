package com.exe101.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopRegistrationEmailRequest {

    @NotBlank(message = "Tiêu đề email không được để trống")
    @Size(max = 255, message = "Tiêu đề email không được vượt quá 255 ký tự")
    private String title;

    @NotBlank(message = "Nội dung email không được để trống")
    @Size(max = 5000, message = "Nội dung email không được vượt quá 5000 ký tự")
    private String content;
}
