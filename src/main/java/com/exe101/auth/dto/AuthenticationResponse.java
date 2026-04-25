package com.exe101.auth.dto;

import com.exe101.user.dto.UserDTO;
import com.exe101.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {
    private String accessToken;
    private UserRole role;
    private String refreshToken;
    private UserDTO user;
    private List<AuthenticatedShopDTO> shops;
    private Long currentShopId;

    public AuthenticationResponse(String accessToken, UserRole role, String refreshToken, UserDTO user) {
        this(accessToken, role, refreshToken, user, List.of(), null);
    }
}
