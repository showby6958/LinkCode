package com.example.auth.security;

import com.example.auth.oauth.service.CustomOAuth2UserService;
import com.example.common.security.JwtAuthenticationFilter;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler successHandler;
    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
                )
                .addFilterBefore((Filter) jwtFilter, UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }

    // CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000")); //프론트 주소
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); // HTTP 메서드 허용
        config.setAllowedHeaders(List.of("*")); // 모든 헤더 허용
        config.setAllowCredentials(true); // 쿠키 허용
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
