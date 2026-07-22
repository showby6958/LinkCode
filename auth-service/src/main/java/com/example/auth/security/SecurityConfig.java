package com.example.auth.security;

import com.example.auth.oauth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler successHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // CORS는 게이트웨이가 중앙에서 처리
                .cors(cors -> cors.disable())
                .authorizeHttpRequests(auth -> auth
                        // JWKS는 공개키만 담고 있고, 게이트웨이가 토큰 없이 호출하므로 열어둔다.
                        .requestMatchers("/oauth/.well-known/**").permitAll()
                        .requestMatchers("/oauth/me", "/oauth/reissue", "/oauth/logout", "/login/**", "/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable())
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(successHandler)
                );
        // 토큰 검증 필터를 두지 않는다. 요청의 신원 확인은 게이트웨이가 담당하고,
        // 이 서비스는 게이트웨이가 넣어준 X-User-* 헤더를 읽는다(AuthController).
        // oauth2Login은 로그인 처리에 필요하므로 SecurityConfig 자체는 유지한다.

        return http.build();
    }
}
