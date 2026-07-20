package com.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * 경로 → 서비스 라우팅과 CORS를 Java로 정의한다.
 * yml의 spring.cloud.gateway.* 프로퍼티 접두어가 Spring Cloud 2025.0에서 바뀌어,
 * 버전 변화에 흔들리지 않도록 코드로 둔다.
 */
@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               @Value("${services.auth-uri}") String authUri,
                               @Value("${services.interview-uri}") String interviewUri) {
        return builder.routes()
                // 로그인/OAuth/JWKS → auth-service (토큰 없이 시작하므로 필터가 차단하지 않음)
                .route("auth", r -> r
                        .path("/oauth/**", "/oauth2/**", "/login/**")
                        .uri(authUri))
                // WebSocket → interview-service. Upgrade 헤더가 있으면 게이트웨이가
                // ws 프록시로 자동 전환한다. 핸드셰이크 쿠키로 필터가 사용자 헤더를 주입한다.
                .route("interview-ws", r -> r
                        .path("/ws/**", "/ws-code/**")
                        .uri(interviewUri))
                // interview REST API → interview-service (보호 경로: 필터가 401/403 처리)
                .route("interview-api", r -> r
                        .path("/api/**", "/interview/**")
                        .uri(interviewUri))
                .build();
    }

    @Bean
    public CorsWebFilter corsWebFilter(@Value("${gateway.cors.allowed-origins}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        // 프론트가 별도 오리진이므로 자격증명(쿠키) 포함 요청을 허용한다.
        config.setAllowedOrigins(Arrays.asList(origins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
