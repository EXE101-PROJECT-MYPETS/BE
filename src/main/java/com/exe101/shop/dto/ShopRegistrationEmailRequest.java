package com.exe101.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopRegistrationEmailRequest {

    @NotBlank(message = "Tieu de email khong duoc de trong")
    @Size(max = 255, message = "Tieu de email khong duoc vuot qua 255 ky tu")
    private String title;

    @NotBlank(message = "Noi dung email khong duoc de trong")
    @Size(max = 5000, message = "Noi dung email khong duoc vuot qua 5000 ky tu")
    private String content;
}
