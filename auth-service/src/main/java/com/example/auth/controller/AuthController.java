package com.example.auth.controller;

import com.example.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
    // 토큰 검증은 게이트웨이가 마쳤고, 그 결과가 X-User-* 헤더로 들어온다.
    // 헤더가 없으면 = 게이트웨이가 유효한 토큰을 찾지 못한 요청이므로 401.
    // 값은 한글 이름이 헤더(ASCII)에서 깨지지 않도록 Base64-URL로 인코딩되어 온다.
    @GetMapping("/me")
    public ResponseEntity<?> getMe(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                   @RequestHeader(value = "X-User-Name", required = false) String userName,
                                   @RequestHeader(value = "X-User-Picture", required = false) String picture,
                                   @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", Long.valueOf(decode(userId)));
        result.put("userName", decode(userName));
        result.put("picture", decode(picture));
        result.put("role", decode(role));

        return ResponseEntity.ok(result);
    }

    private String decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
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
