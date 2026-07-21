package com.example.interview.security;

import java.security.Principal;

/**
 * 게이트웨이가 검증해 넘겨준 사용자 정보. 기존 CustomPrincipal(common-module)을 대체한다.
 *
 * <p>interview-service는 토큰을 파싱하지 않는다. 게이트웨이가 이미 검증한 뒤
 * X-User-* 헤더로 넣어준 값을 그대로 담을 뿐이다.
 *
 * <p>{@link Principal}(JDK 표준)을 구현한다. 그래서 STOMP 연결의 사용자로 이 객체를
 * 그대로 set 할 수 있고, 스프링 시큐리티의 Authentication으로 감쌀 필요가 없다.
 * (덕분에 interview-service는 스프링 시큐리티 의존성을 두지 않는다)
 */
public record LoginUser(
        Long userId,
        String email,
        String userName,
        String picture,
        String role
) implements Principal {

    // Principal.getName() — 사용자 식별자로 userId를 쓴다.
    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
