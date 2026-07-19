package com.example.auth.security;

import com.example.auth.oauth.userinfo.CustomOAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> authRedisTemplate;
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        System.out.println("Success Handler 실행됨");
        CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        Long userId = customOAuth2User.getUserId();
        String email = customOAuth2User.getEmail();
        String userName = customOAuth2User.getName();
        String picture = customOAuth2User.getPicture();
        String role = customOAuth2User.getRoleValue();


        // 1. 토큰 생성 (JwtProvider)
        String accessToken = jwtTokenProvider.createAccessToken(userId, email, userName, picture, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);


        // 2. Redis 저장
        authRedisTemplate.opsForValue().set(
                "refresh:" + userId,
                refreshToken,
                7, TimeUnit.DAYS
        );

        // 3. 쿠키 처리(JwtUtil)
        jwtUtil.addAccessToken(response, accessToken);
        jwtUtil.addRefreshToken(response, refreshToken);

        // 로그
        System.out.println("쿠키 생성하고 리다이랙트까지 됨");

        // 리다이랙트 - 프론트 callback
        response.sendRedirect("http://localhost:3000");
    }
}
