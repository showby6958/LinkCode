package com.example.auth.service;

import com.example.auth.domain.Member;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.JwtUtil;
import com.example.common.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> authRedisTemplate;
    private final JwtUtil jwtUtil;

    private static final String REFRESH_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    // AccessToken 재발급
    public void reissue(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        String type = jwtTokenProvider.parse(refreshToken).get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new IllegalArgumentException("리프레시 토큰이 아닙니다.");
        }

        // 블랙리스트 확인
        if (Boolean.TRUE.equals(authRedisTemplate.hasKey(BLACKLIST_PREFIX + refreshToken))) {
            throw new IllegalArgumentException("로그아웃된 토큰입니다.");
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);

        // Redis에 저장된 토큰과 비교
        String storedToken = authRedisTemplate.opsForValue().get(REFRESH_PREFIX + userId);
        if (!refreshToken.equals(storedToken)) {
            throw new IllegalArgumentException("토큰 정보가 일치하지 않습니다.");
        }

        Member member = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String newAccessToken = jwtTokenProvider.createAccessToken(
                member.getId(), member.getEmail(), member.getName(), member.getPicture(), member.getRole().name()
        );

        jwtUtil.addAccessToken(response, newAccessToken);
        log.info("AccessToken 재발급 완료 - userId: {}", userId);
    }

    // 로그아웃 (RefreshToken 블랙리스트 등록 + 쿠키 삭제)
    public void logout(String refreshToken, HttpServletResponse response) {
        if (refreshToken != null) {
            try {
                Long userId = jwtTokenProvider.getUserIdIgnoreExpiry(refreshToken);

                // 유효한 토큰이면 블랙리스트에 등록 (남은 만료 시간만큼 TTL 설정)
                if (jwtTokenProvider.validateToken(refreshToken)) {
                    Date expiration = jwtTokenProvider.getExpiration(refreshToken);
                    long remainingMillis = expiration.getTime() - System.currentTimeMillis();
                    if (remainingMillis > 0) {
                        authRedisTemplate.opsForValue().set(
                                BLACKLIST_PREFIX + refreshToken,
                                "logout",
                                remainingMillis,
                                TimeUnit.MILLISECONDS
                        );
                    }
                }

                // Redis에서 RefreshToken 삭제
                authRedisTemplate.delete(REFRESH_PREFIX + userId);
                log.info("로그아웃 완료 - userId: {}", userId);

            } catch (Exception e) {
                log.warn("로그아웃 처리 중 토큰 파싱 실패: {}", e.getMessage());
            }
        }

        // 쿠키 삭제
        jwtUtil.deleteCookie(response, "accessToken");
        jwtUtil.deleteCookie(response, "refreshToken");
    }
}
