package com.example.auth.controller;

import com.example.auth.security.RsaKeyProvider;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// JWK Set 공개. 게이트웨이가 이 공개키로 토큰 서명을 직접 검증한다(auth 호출 없이).
@RestController
@RequiredArgsConstructor
public class JwkSetController {

    private final RsaKeyProvider rsaKeyProvider;

    // 인증 없이 접근 가능. 공개키만 공개
    @GetMapping("/oauth/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        RSAKey key = new RSAKey.Builder(rsaKeyProvider.getPublicKey())
                .keyID(rsaKeyProvider.getKeyId())   // JWT 헤더의 kid와 짝을 이룬다
                .algorithm(JWSAlgorithm.RS256)
                .keyUse(KeyUse.SIGNATURE)
                .build();

        // Builder에 공개키만 넣었고 toPublicJWK()로 한 번 더 거른다.
        // 개인키 성분(d, p, q 등)이 응답에 절대 실리지 않도록 하기 위함.
        return new JWKSet(key.toPublicJWK()).toJSONObject();
    }
}
