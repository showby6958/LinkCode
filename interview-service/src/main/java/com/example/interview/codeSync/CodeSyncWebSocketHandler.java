package com.example.interview.codeSync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeSyncWebSocketHandler extends BinaryWebSocketHandler {

    // 메모리 상에서 방별로 연결된 웹소켓 세션들을 관리 (Concurrent HashMap 사용)
    private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    // Y.js 바이너리, 평문 Snapshot을 임시 저장할 RedisTemplate
    private final RedisTemplate<String, byte[]> codeSyncRedisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String REDIS_KEY_PREFIX = "interview:room:";

    // **클라이언트가 웹소켓에 연결되었을 때 (방 입장)**
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long roomId = getRoomId(session);

        // 방에 해당하는 세션 리스트가 없으면 새로 생성하고 세션 추가
        roomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("[WebSocket] 유저 입장 - RoomID: {}, SessionID: {}", roomId, session.getId());

        String redisListKey = REDIS_KEY_PREFIX + roomId + ":yjs";
        String snapshotKey = REDIS_KEY_PREFIX + roomId + ":snapshot";

        // Redis List에 쌓인 모든 바이너리 조각(0번 부터 끝가지)을 가져옴
        List<byte[]> deltaList = codeSyncRedisTemplate.opsForList().range(redisListKey, 0, -1);

        if (deltaList != null && !deltaList.isEmpty()) {
            // CASE 1: 중간 진입 유저 혹은 새로고침 유저 -> 기존 바이너리들을 순서대로 전송
            for (byte[] delta : deltaList) {
                if (session.isOpen()) {
                    session.sendMessage(new BinaryMessage(ByteBuffer.wrap(delta)));
                }
            }

            log.info("[WebSocket] 유저(SessionID: {})에게 델타 히스토리 스트리밍 완료", session.getId());
        } else {
            // CASE 2: 최초 방 입장 -> 기본 빈 에디터 상태 유지 (아무것도 보내지 않음)
            log.info("[WebSocket] 최초 진입 유저 감지 - 빈 에티터로 시작");
        }
    }

    // ** 클라이언트로부터 Y.js 바이너리 조각(Delta)을 수신했을 때 **
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        Long roomId = getRoomId(session);

        ByteBuffer payload = message.getPayload();
        byte[] deltaBytes = new byte[payload.remaining()];
        payload.get(deltaBytes);

        // 1. 나를 제외한 같은 방의 모든 유저에게 바이너리 데이터 브로드캐스트
        broadcastToRoom(roomId, session, deltaBytes);
        
        // 2. Redis List에 Y.js 바이너리 조각을 계속 누적 업데이트
        String redisListKey = REDIS_KEY_PREFIX + roomId + ":yjs";
        
        // 간단한 구현을 위해 수신한 델타 조각을 Redis 레코드 뒤에 이어 붙이는(Append) 전략 사용
        codeSyncRedisTemplate.opsForList().rightPush(redisListKey, deltaBytes);
    }

    // ** 클라이언트 연결이 끊겼을 때 (새로고침, 브라우저 종료 등)
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long roomId = getRoomId(session);
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomId);
            }
        }
        log.info("[WebSocket] 유저 퇴장 = RoomID: {}, SessionID: {}", roomId, session.getId());
    }

    // ** 같은 방 유저들에게 데이터를 뿌리는 헬퍼 메서드 **
    private void broadcastToRoom(Long roomId, WebSocketSession senderSession, byte[] data) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null) return;

        BinaryMessage binaryMessage = new BinaryMessage(ByteBuffer.wrap(data));

        for (WebSocketSession session : sessions) {
            // 본인을 제외한 다른 연결된 세션에만 메시지 발송
            if (session.isOpen() && !session.getId().equals(senderSession.getId())) {
                try {
                    session.sendMessage(binaryMessage);
                } catch (IOException e) {
                    log.error("[WebSocket] 브로드캐스트 실패 - SessionID: {}", session.getId());
                }
            }
        }
    }

    // ** URI에 포함된 룸 ID 추출 헬퍼 메서드 (e.g /ws/code/123 -> 123)
    private Long getRoomId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");

        return Long.parseLong(parts[parts.length - 1]);
    }
}
