package com.exe101.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ShopRegistrationEmailResponse {
    private boolean success;
    private Long shopId;
    private String email;
    private String message;
}
