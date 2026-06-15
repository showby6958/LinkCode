package com.example.interview.codeSync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/interview/room")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class CodeSnapshotController {

    private final StringRedisTemplate stringRedisTemplate;
    private static final String REDIS_KEY_PREFIX = "interview:room:";

    // ** 프론트에서 디바운스(1초)로 현재 에디터의 전체 평문을 쏴주는 API **
    @PatchMapping("/{roomId}/snapshot")
    public void updateCodeSnapshot(
            @PathVariable Long roomId,
            @RequestBody String currentCode
    ) {
        String snapshotKey = REDIS_KEY_PREFIX + roomId + ":snapshot";
        log.info("[Patch] Redis에 최신코드로 snapshot 덮어씀");
        // 최신 코드로 덮어쓰기
        stringRedisTemplate.opsForValue().set(snapshotKey, currentCode);
    }
}
