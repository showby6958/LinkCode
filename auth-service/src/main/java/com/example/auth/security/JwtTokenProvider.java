package com.example.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;


// 토큰 발급, 검증. auth-service 전용 RS256 으로 서명
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final RsaKeyProvider rsaKeyProvider;

    private static final long ACCESS_TOKEN_EXPIRE = 1000L * 60 * 30;              // 30분
    private static final long REFRESH_TOKEN_EXPIRE = 1000L * 60 * 60 * 24 * 4;    // 4일

    // AccessToken
    public String createAccessToken(Long userId, String email, String userName, String picture, String role) {

        Date now = new Date();
        Date validity = new Date(now.getTime() + ACCESS_TOKEN_EXPIRE);

        return Jwts.builder()
                // kid: 게이트웨이가 JWKS에서 검증용 공개키를 찾는 열쇠
                .header().keyId(rsaKeyProvider.getKeyId()).and()

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

                // 개인키로 서명 (공개키로만 검증 가능)
                .signWith(rsaKeyProvider.getPrivateKey(), Jwts.SIG.RS256)

                .compact();
    }

    // Refresh Token
    public String createRefreshToken(Long userId) {

        Date now = new Date();
        Date validity = new Date(now.getTime() + REFRESH_TOKEN_EXPIRE);

        return Jwts.builder()
                .header().keyId(rsaKeyProvider.getKeyId()).and()

                // 표준 클레임
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(validity)

                // 커스텀 클레임
                .claim("userId", userId) // 재발급 시 Redis 키(refresh:{userId})를 찾기 위해
                .claim("type", "refresh")

                .signWith(rsaKeyProvider.getPrivateKey(), Jwts.SIG.RS256)

                .compact();
    }

    // Claim에서 userId 추출
    public Long getUserId(String token) {
        return parse(token).get("userId", Long.class);
    }

    // Claims 파싱 (공개키로 서명 검증)
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(rsaKeyProvider.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Date getExpiration(String token) {
        return parse(token).getExpiration();
    }

    // 만료된 토큰에서도 userId 추출 (로그아웃 처리용)
    public Long getUserIdIgnoreExpiry(String token) {
        try {
            return parse(token).get("userId", Long.class);
        } catch (ExpiredJwtException e) {
            return e.getClaims().get("userId", Long.class);
        }
    }

    // 토큰 문자열의 유효성 검증
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("만료된 JWT 토큰");
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("유효하지 않은 JWT: {}", e.getMessage());
        }
        return false;
    }
}
