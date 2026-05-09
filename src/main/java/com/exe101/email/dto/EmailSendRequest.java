package com.exe101.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailSendRequest {

    @Email(message = "Email người nhận không đúng định dạng")
    @NotBlank(message = "Email người nhận không được để trống")
    private String to;

    @NotBlank(message = "Tiêu đề email không được để trống")
    private String subject;

    @NotBlank(message = "Nội dung email không được để trống")
    private String body;

    private Boolean html;
}
