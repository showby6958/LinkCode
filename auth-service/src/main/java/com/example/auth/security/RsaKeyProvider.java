package com.example.auth.security;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

// RS256 서명에 쓰이는 RSA 키페어 보관. 게이트웨이는 JWKS로 공개키만 받아 검증.
@Component
@Getter
@Slf4j
public class RsaKeyProvider {

    private static final int KEY_SIZE = 2048;

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final String keyId;

    public RsaKeyProvider() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_SIZE);
            KeyPair pair = generator.generateKeyPair();

            this.publicKey = (RSAPublicKey) pair.getPublic();
            this.privateKey = (RSAPrivateKey) pair.getPrivate();
            this.keyId = UUID.randomUUID().toString();
        } catch (NoSuchAlgorithmException e) {
            // RSA는 JDK 표준이라 실제로는 발생하지 않는다.
            throw new IllegalStateException("RSA 키페어 생성에 실패했습니다.", e);
        }

        // 기동할 때마다 kid가 바뀐다. 토큰이 갑자기 무효가 될 때 이 로그로 원인을 확인한다.
        log.info("RSA 키페어 생성 완료 (kid={}). 재시작 전 발급된 토큰은 모두 무효화됩니다.", keyId);
    }
}
