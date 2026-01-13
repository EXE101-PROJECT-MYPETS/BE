package com.exe101.auth.service;

import com.exe101.auth.dto.AuthenticationRequest;
import com.exe101.auth.dto.AuthenticationResponse;
import com.exe101.auth.dto.RegisterRequest;
import com.exe101.auth.model.RefreshToken;
import com.exe101.user.entity.User;
import com.exe101.user.entity.UserRole;
import com.exe101.user.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.exe101.auth.exception.LoginException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

   private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    public AuthenticationResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException(request.getEmail() + " already exists!");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.STUDENT);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken =
                refreshTokenService.create(user.getId());

        return new AuthenticationResponse(
                accessToken,
                user.getRole(),
                refreshToken.getToken()
        );
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            throw new LoginException(
                    "WrongPassOrEmail",
                    "Sai email hoặc mật khẩu"
            );
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken =
                refreshTokenService.create(user.getId());

        return new AuthenticationResponse(
                accessToken,
                user.getRole(),
                refreshToken.getToken()
        );
    }

    public void logout(String refreshToken) {
        refreshTokenService.revokeByToken(refreshToken);
    }
}
