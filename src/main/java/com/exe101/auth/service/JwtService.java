package com.exe101.auth.service;
import com.exe101.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private final String secretKey;
    private final long accessExpMs;

    public JwtService(
            @Value("${jwt.secret-key}") String secretKey,
            @Value("${app.jwt.access-exp-ms:900000}") long accessExpMs
    ) {
        this.secretKey = secretKey;
        this.accessExpMs = accessExpMs;
    }

    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpireDate(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private boolean isTokenExpired(String token) {
        return extractExpireDate(token).before(new Date());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String email = extractUserName(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        if (userDetails instanceof User user) {
            claims.put("role", user.getRole().name());
        }
        return generateToken(claims, userDetails);
    }

    private String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetail
    ) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetail.getUsername())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + accessExpMs))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Tách token: Header.Payload.Signature
    public Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Key getSignInKey() {
        // decode base64 secret key → byte[]
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        // tạo HMAC-SHA key dùng cho sign & verify
        return Keys.hmacShaKeyFor(keyBytes);
    }
}