package com.example.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

// auth-service의 JWKS로 토큰 서명을 검증하는 디코더
// auth-service를 매 요청 호출하지 않음. 공개키를 한 번 받아서 캐시해두고 게이트웨이가 스스로 검증.
@Configuration
public class JwtDecoderConfig {

    //  검증 로직을 직접 만들지 않음 - JWKS 받기, 서명, 만료, 캐싱 등 Nimbus 디코더에서 전부 진행.
    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${gateway.jwks-uri}") String jwkSetUri) {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
