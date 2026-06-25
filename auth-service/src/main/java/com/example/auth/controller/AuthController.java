package com.example.auth.controller;

import com.example.auth.service.AuthService;
import com.example.common.security.CustomPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/oauth")
public class AuthController {

    private final AuthService authService;

    // 1. OAuth 로그인 (프론트에서 호출)
    @GetMapping("/login/{provider}")
    public void redirectToProvider(@PathVariable String provider,
                                   HttpServletResponse response) throws IOException {
        if (!List.of("google", "kakao", "naver").contains(provider)) {
            throw new IllegalArgumentException("지원하지 않는 provider");
        }
        response.sendRedirect("/oauth2/authorization/" + provider);
    }

    // 2. 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal CustomPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", principal.getUserId());
        result.put("userName", principal.getUserName());
        result.put("picture", principal.getPicture());
        result.put("role", principal.getRole());

        return ResponseEntity.ok(result);
    }

    // 3. AccessToken 재발급
    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, "refreshToken");
        try {
            authService.reissue(refreshToken, response);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // 4. 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, "refreshToken");
        authService.logout(refreshToken, response);
        return ResponseEntity.ok().build();
    }

    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
