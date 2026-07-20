package com.example.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * 모든 요청에 대해 순서대로:
 * <ol>
 *   <li>클라이언트가 보낸 X-User-* 헤더를 <b>제거</b>한다. 이 단계가 빠지면
 *       누구나 X-User-Role: ROLE_ADMIN 을 붙여 관리자로 행세할 수 있다.</li>
 *   <li>accessToken 쿠키를 꺼내 서명·만료를 검증한다(JWKS 공개키).</li>
 *   <li>보호 경로인데 토큰이 없거나 무효면 401로 끊는다.</li>
 *   <li>유효하면 검증된 값만 X-User-* 헤더로 넣어 다운스트림에 넘긴다.</li>
 * </ol>
 *
 * <p>로그인 경로(/oauth, /login)는 토큰 없이 시작하므로 차단하지 않는다.
 * WebSocket 경로도 차단하지 않되, 핸드셰이크 요청에 쿠키가 실려 오므로
 * 위 4단계는 그대로 적용되어 사용자 정보가 헤더로 전달된다.
 *
 * <p>헤더 값은 Base64-URL로 인코딩한다. 사용자 이름에 한글이 들어가면
 * HTTP 헤더(ASCII)에서 깨지기 때문이며, 보호 목적이 아니다.
 */
@Component
public class JwtHeaderInjectionFilter implements GlobalFilter, Ordered {

    public static final String H_USER_ID = "X-User-Id";
    public static final String H_EMAIL = "X-User-Email";
    public static final String H_NAME = "X-User-Name";
    public static final String H_PICTURE = "X-User-Picture";
    public static final String H_ROLE = "X-User-Role";

    private static final List<String> ALL_USER_HEADERS =
            List.of(H_USER_ID, H_EMAIL, H_NAME, H_PICTURE, H_ROLE);

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";

    private final ReactiveJwtDecoder jwtDecoder;

    public JwtHeaderInjectionFilter(ReactiveJwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    // 모든 요청의 관문. 위조 헤더 제거 → 토큰 검증 → 역할 검사 → 헤더 주입/차단
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. 위조 차단: 클라이언트가 보낸 X-User-* 를 먼저 지운다.
        ServerHttpRequest stripped = exchange.getRequest().mutate()
                .headers(headers -> ALL_USER_HEADERS.forEach(headers::remove))
                .build();

        String token = resolveToken(exchange);
        if (token == null) {
            return isProtected(path) ? unauthorized(exchange) : forward(exchange, chain, stripped);
        }

        HttpMethod method = exchange.getRequest().getMethod();

        // 2~4. 검증에 성공하면 역할 검사 후 헤더 주입, 실패하면 보호 경로만 401.
        return jwtDecoder.decode(token)
                .flatMap(jwt -> {
                    // 경로별 역할 규칙 위반이면 403. (예: 면접방 생성은 ROLE_ADMIN만)
                    if (!hasRequiredRole(method, path, claim(jwt, "role"))) {
                        return forbidden(exchange);
                    }
                    return forward(exchange, chain, withUserHeaders(stripped, jwt));
                })
                .onErrorResume(e ->
                        isProtected(path) ? unauthorized(exchange) : forward(exchange, chain, stripped));
    }

    /** 보호 경로 = interview REST API. 로그인·WS는 여기서 차단하지 않는다. */
    private boolean isProtected(String path) {
        return path.startsWith("/api") || path.startsWith("/interview");
    }

    /** 경로별 역할 규칙을 모두 만족하는가. 해당 규칙이 없으면 통과. */
    private boolean hasRequiredRole(HttpMethod method, String path, String role) {
        return RoleRule.RULES.stream()
                .filter(rule -> rule.matches(method, path))
                .allMatch(rule -> rule.requiredRole().equals(role));
    }

    // 검증된 토큰의 클레임을 X-User-* 헤더로 실어 하위 서비스로 넘길 요청을 만든다.
    private ServerHttpRequest withUserHeaders(ServerHttpRequest request, Jwt jwt) {
        return request.mutate()
                .header(H_USER_ID, encode(claim(jwt, "userId")))
                .header(H_EMAIL, encode(claim(jwt, "email")))
                .header(H_NAME, encode(claim(jwt, "userName")))
                .header(H_PICTURE, encode(claim(jwt, "picture")))
                .header(H_ROLE, encode(claim(jwt, "role")))
                .build();
    }

    // 가공한 요청으로 교체해 다음 필터·라우팅으로 흘려보낸다.
    private Mono<Void> forward(ServerWebExchange exchange, GatewayFilterChain chain, ServerHttpRequest request) {
        return chain.filter(exchange.mutate().request(request).build());
    }

    // 인증 실패(토큰 없음/무효)로 응답을 401로 끝낸다.
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    // 인가 실패(권한 부족)로 응답을 403으로 끝낸다.
    private Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    // accessToken 쿠키에서 토큰 문자열을 꺼낸다(없으면 null).
    private String resolveToken(ServerWebExchange exchange) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(ACCESS_TOKEN_COOKIE);
        return cookie != null ? cookie.getValue() : null;
    }

    // 클레임 값을 문자열로 꺼낸다(없으면 빈 문자열).
    private String claim(Jwt jwt, String name) {
        Object value = jwt.getClaim(name);
        return value == null ? "" : String.valueOf(value);
    }

    // 헤더 값을 Base64-URL로 인코딩한다(한글 등이 ASCII 헤더에서 깨지지 않도록).
    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    // 이 필터의 실행 우선순위를 정한다(라우팅보다 먼저여야 헤더 주입이 반영됨).
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
