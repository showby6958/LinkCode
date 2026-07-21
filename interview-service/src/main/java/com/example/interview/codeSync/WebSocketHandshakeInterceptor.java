package com.example.interview.codeSync;

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
 * /ws-code/{roomId} (Y.js 코드 동기화, 원시 WebSocket) 핸드셰이크 인증.
 *
 * <p>핸드셰이크는 HTTP GET(+Upgrade)이라 게이트웨이가 X-User-* 헤더를 넣어준다.
 * 여기서는 그 헤더를 읽어 세션 attributes에 담아, 핸들러가 "누가 편집 중인지" 알게 한다.
 * JWT는 파싱하지 않는다.
 *
 * <p>이전 구현은 {@code return true}만 하고 아무 검증도 없어, 게이트웨이를 우회하면
 * 누구나 남의 코드에 접속할 수 있었다. 헤더가 없으면 핸드셰이크를 거부한다.
 */
@Component
@Slf4j
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

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
            log.warn("/ws-code 핸드셰이크 거부: 사용자 헤더 없음(비로그인)");
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
