package com.example.interview.infrastructure.websocket;

import com.example.interview.security.LoginUser;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * STOMP CONNECT 시 세션에 실린 사용자 정보를 STOMP principal로 붙인다.
 *
 * <p>JWT 파싱은 하지 않는다. 핸드셰이크 때 {@link JwtHandshakeInterceptor}가
 * 게이트웨이 헤더에서 만든 {@link LoginUser}를 세션 attributes에 담아 두었고,
 * 여기서는 그것을 꺼내 principal로 세팅할 뿐이다. LoginUser가 Principal을
 * 구현하므로 스프링 시큐리티 Authentication으로 감싸지 않는다.
 *
 * <p>이후 STOMP 컨트롤러는 이 principal로 "누가 보낸 메시지인지"를 안다.
 */
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || StompCommand.CONNECT != accessor.getCommand()) {
            return message;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        Object attr = sessionAttributes == null
                ? null
                : sessionAttributes.get(JwtHandshakeInterceptor.LOGIN_USER_ATTR);

        // 핸드셰이크에서 이미 헤더를 검사해 거부했으므로 정상 흐름에선 항상 존재한다.
        if (!(attr instanceof LoginUser user)) {
            throw new IllegalStateException("인증 정보가 없는 STOMP 연결입니다.");
        }

        accessor.setUser(user);
        return message;
    }
}
