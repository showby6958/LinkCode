package com.example.auth.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    public void addAccessToken(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("accessToken", token)
                .path("/")
                .httpOnly(false)
                .secure(false) // 개발 환경(http) false
                .sameSite("Lax") // 쿠키가 상위 레벨 탐색(링크 클릭)을 통해서만 전송
                .maxAge(60 * 30) // 30분
                .build();

        // HttpServletResponse 헤더에 주입
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void addRefreshToken(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
                .path("/")
                .httpOnly(true) // RefreshToken은 탈취 방지를 위해 HttpOnly
                .secure(false) // 개발환경(http) false
                .sameSite("Lax") // 쿠키가 상위 레벨 탐색(링크 클릭)을 통해서만 전송
                .maxAge(60 * 60 * 24 * 7) // 7일
                .build();

        // HttpServletResponse 헤더에 주입
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void deleteCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .path("/")
                .maxAge(0) // 만료 시간을 0으로 세팅해서 즉시 삭제
                .secure(false)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
