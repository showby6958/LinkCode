package com.example.interview.infrastructure.websocket;

import com.example.common.security.CustomPrincipal;
import com.example.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

// STOMP native header의 "Authorization"을 읽어서 JWT 가져옴
// (만약, JWT가 쿠키에 저장되어있으면 못가져옴 -> 그래서 HandshakeInterceptor 사용)
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
        );

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT == accessor.getCommand()) {
            String token = resolveToken(accessor);

            if (!StringUtils.hasText(token)) {
                throw new IllegalArgumentException("Authorization token is missing.");
            }

            if (!jwtTokenProvider.validateToken(token)) {
                throw new IllegalArgumentException("Invalid authorization token.");
            }

            String tokenType = jwtTokenProvider.parse(token).get("type", String.class);
            if (!"access".equals(tokenType)) {
                throw new IllegalArgumentException("Only access token is allowed.");
            }

            Long userId = jwtTokenProvider.getUserId(token);
            String email = jwtTokenProvider.getEmail(token);
            String userName = jwtTokenProvider.getUserName(token);
            String picture = jwtTokenProvider.getPicture(token);
            String role = jwtTokenProvider.getRole(token);

            // CustomPrincipal에 유저 정보 저장
            CustomPrincipal principal = new CustomPrincipal(
                    userId,
                    email,
                    userName,
                    picture,
                    role
            );

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority(role))
            );

            accessor.setUser(authentication);
        }

        return message;
    }

    // ChannelInterceptor에서 header 먼저 보고, 없으면 session attribute 확인
    private String resolveToken(StompHeaderAccessor accessor) {
        String authorizationHeader = accessor.getFirstNativeHeader("Authorization");

        // Authorization header로 보낸 JWT
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        if (sessionAttributes == null) {
            return null;
        }

        // 쿠키 accessToken에 저장된 JWT
        Object token = sessionAttributes.get("accessToken");

        if (token instanceof String accessToken && StringUtils.hasText(accessToken)) {
            return accessToken;
        }

        return null;
    }
}
