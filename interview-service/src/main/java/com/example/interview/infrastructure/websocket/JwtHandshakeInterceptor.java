package com.example.interview.infrastructure.websocket;

import com.example.interview.security.HeaderUserCodec;
import com.example.interview.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * /ws (STOMP) 핸드셰이크 인증.
 *
 * <p>핸드셰이크는 HTTP GET(+Upgrade)이므로 게이트웨이의 전역 필터가 이미 토큰을
 * 검증하고 X-User-* 헤더를 넣어준다. 여기서는 그 헤더를 읽어 LoginUser로 만들고
 * 세션 attributes에 담아, 뒤이어 오는 STOMP CONNECT 프레임이 쓰게 한다.
 * (이전에는 이 인터셉터가 쿠키에서 JWT를 꺼내 저장했다 — 이제 파싱하지 않는다)
 *
 * <p>헤더가 없으면 = 로그인하지 않은 접속이므로 핸드셰이크를 거부한다.
 */
@Component
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String LOGIN_USER_ATTR = "loginUser";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        LoginUser user = HeaderUserCodec.from(
                request.getHeaders().getFirst(HeaderUserCodec.H_USER_ID),
                request.getHeaders().getFirst(HeaderUserCodec.H_EMAIL),
                request.getHeaders().getFirst(HeaderUserCodec.H_NAME),
                request.getHeaders().getFirst(HeaderUserCodec.H_PICTURE),
                request.getHeaders().getFirst(HeaderUserCodec.H_ROLE)
        );

        if (user == null) {
            log.warn("/ws 핸드셰이크 거부: 사용자 헤더 없음(비로그인)");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put(LOGIN_USER_ATTR, user);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }
}
