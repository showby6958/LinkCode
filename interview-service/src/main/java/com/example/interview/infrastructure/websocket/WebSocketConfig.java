package com.example.interview.infrastructure.websocket;

import com.example.interview.codeSync.CodeSyncWebSocketHandler;
import com.example.interview.codeSync.WebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final CodeSyncWebSocketHandler codeSyncWebSocketHandler;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 구독 (sub)
        config.enableSimpleBroker("/topic");
        // 발행 (sub)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(jwtHandshakeInterceptor) // WebSocket 연결 전에 interceptor에서 JWT 인증 처리하는 interceptor
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor); // 클라이언트가 서버로 보내는 STOMP 메시지를 가로채는 interceptor
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(codeSyncWebSocketHandler, "/ws-code/{roomId}")
                .setAllowedOrigins("http://localhost:3000", "http://localhost:5500") // 테스트용 전체 허용 (운영 환경에서는 프론트 도메인만 지정)
                .addInterceptors(new WebSocketHandshakeInterceptor());
    }
}
