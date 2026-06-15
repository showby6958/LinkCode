package com.example.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    private final String SECRET_KEY = "this-is-a-very-long-secret-key-for-jwt-256bit";

    private final long ACCESS_TOKEN_EXPIRE = 1000 * 60 * 30; // 30분
    private final long REFRESH_TOKEN_EXPIRE = 1000 * 60 * 60 * 24 * 4; // 4일

    SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    // AccessToken
    public String createAccessToken(Long userId, String email, String userName, String picture, String role) {

        Date now = new Date();
        Date validity = new Date(now.getTime() + ACCESS_TOKEN_EXPIRE);

        return Jwts.builder()
                // 표준 클레임
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(validity)

                // 커스텀 클레임
                .claim("userId", userId)
                .claim("email", email)
                .claim("userName", userName)
                .claim("picture", picture)
                .claim("role", role)
                .claim("type", "access")

                // 서명 설정
                .signWith(key) // 0.12.x부터는 알고리즘 생략 시 키 크기에 맞춰 자동 선택됨

                .compact();
    }

    // Refresh Token
    public String createRefreshToken(Long userId) {

        Date now = new Date();
        Date validity = new Date(now.getTime() + REFRESH_TOKEN_EXPIRE);

        return Jwts.builder()
                // 표준 클레임
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(validity)

                // 커스텀 클레임
                .claim("userId", userId) // 토큰 재발급 시 Redis 키를 찾기 위해서 (편의성) // RefreshToken:{userId}
                .claim("type", "refresh")

                // 서명 설정
                .signWith(key) // 0.12.x부터는 알고리즘 생략 시 키 크기에 맞춰 자동 선택됨

                .compact();
    }


    // Claim에서 userId 추출
    public Long getUserId(String token) {
        return parse(token).get("userId", Long.class);
    }

    // Claim에서 email 추출
    public String getEmail(String token) {
        return parse(token).get("email", String.class);
    }

    // Claim에서 userName 추출
    public String getUserName(String token) {
        return parse(token).get("userName", String.class);
    }

    // Claim에서 picture 추출
    public String getPicture(String token) {
        return parse(token).get("picture", String.class);
    }

    public String getRole(String token) {
        return parse(token).get("role", String.class);
    }

    // Claims 파싱
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 토큰 문자열의 유효성 검증
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명", e);
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰", e);
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT", e);
        } catch (IllegalArgumentException e) {
            log.error("JWT claims 문자열이 비어있음", e);
        }
        return false;
    }
}
