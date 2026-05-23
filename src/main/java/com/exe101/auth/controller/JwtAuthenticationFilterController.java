package com.exe101.auth.controller;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.auth.service.JwtService;
import com.exe101.auth.util.ResponseUtil;
import com.exe101.user.entity.User;
import com.exe101.user.repository.IUserRepository;
import com.exe101.userCredential.entity.UserCredential;
import com.exe101.userCredential.repository.IUserCredentialRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilterController extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final IUserRepository userRepository;
    private final IUserCredentialRepository credentialRepository;

    public JwtAuthenticationFilterController(
            JwtService jwtService,
            IUserRepository userRepository,
            IUserCredentialRepository credentialRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
    }

    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI() != null ? request.getRequestURI() : "";

        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/api/auth/")
                || "/graphql".equals(path);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = authHeader.substring(7);
            String userEmail = jwtService.extractUserName(jwt);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userRepository.findByEmail(userEmail)
                        .orElseThrow(() -> new JwtException("Người dùng không tồn tại"));

                UserCredential credential = credentialRepository.findById(user.getId())
                        .orElseThrow(() -> new JwtException("Không tìm thấy thông tin đăng nhập"));

                UserPrincipal userPrincipal = new UserPrincipal(user, credential);

                if (jwtService.isTokenValid(jwt, userPrincipal)) {
                    Claims claims = jwtService.extractAllClaims(jwt);
                    String role = claims.get("role", String.class);
                    if (role == null || role.isBlank()) {
                        role = user.getRole().name();
                    }

                    // Giữ quyền theo claim trong token để không bị lệch khi role thay đổi
                    List<GrantedAuthority> authorities =
                            List.of(new SimpleGrantedAuthority("ROLE_" + role));

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userPrincipal,
                                    null,
                                    authorities
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            ResponseUtil.writeError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Phien dang nhap da het han"
            );
        } catch (JwtException e) {
            ResponseUtil.writeError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Token khong hop le"
            );
        } catch (Exception e) {
            ResponseUtil.writeError(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "Token không hợp lệ"
            );
        }
    }
}
