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
    // 헤더 기반 인증을 하도록 바뀌며 빈으로 승격돼 주입받는다.
    private final WebSocketHandshakeInterceptor codeSyncHandshakeInterceptor;

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
                .setAllowedOriginPatterns("*") // 핸드셰이크 Origin 검증(게이트웨이가 Origin을 그대로 전달)
                .addInterceptors(jwtHandshakeInterceptor) // 게이트웨이 헤더를 읽어 사용자 정보를 세션에 저장
                .withSockJS()
                // SockJS는 /ws/info HTTP 폴백에 자체 CORS 헤더를 붙인다. 게이트웨이도
                // 붙이므로 헤더가 2개가 되어 브라우저가 응답을 거부한다(연결 불가).
                // CORS는 게이트웨이에 일임하고 여기서는 붙이지 않는다.
                .setSuppressCors(true);
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
