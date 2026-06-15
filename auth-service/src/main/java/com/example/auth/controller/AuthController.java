package com.example.auth.controller;

import com.example.auth.oauth.userinfo.CustomOAuth2User;
import com.example.auth.service.AuthService;
import com.example.common.security.CustomPrincipal;
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
        // provider 검증
        if (!List.of("google", "kakao", "naver").contains(provider)) {
            throw new IllegalArgumentException("지원하지 않는 provider");
        }

        // spring security OAuth endpoint로 리다이랙트
        String redirectUrl = "/oauth2/authorization/" + provider;
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal CustomPrincipal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("userId", principal.getUserId());
        response.put("userName", principal.getUserName());
        response.put("picture", principal.getPicture());
        response.put("role", principal.getRole());

        return ResponseEntity.ok(response);
    }
}
