package com.exe101.auth.dto;

import com.exe101.shop.dto.ShopDTO;
import com.exe101.user.dto.UserDTO;
import com.exe101.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopOwnerRegistrationResponse {
    private String accessToken;
    private UserRole role;
    private String refreshToken;
    private UserDTO user;
    private ShopDTO shop;
}
