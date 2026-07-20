package com.example.gateway.filter;

import org.springframework.http.HttpMethod;

import java.util.List;

/**
 * 경로별 역할 요구 규칙.
 *
 * <p>원래 interview-service의 컨트롤러에 붙어 있던 {@code @PreAuthorize("hasRole('ADMIN')")}를
 * 게이트웨이로 옮긴 것이다. interview-service는 보안 로직을 갖지 않으므로, 역할 검사는
 * 인증을 담당하는 이곳에 모은다.
 *
 * <p>규칙을 추가하려면 {@link #RULES} 목록에만 항목을 넣으면 된다.
 *
 * @param method     HTTP 메서드 (null이면 메서드 무관)
 * @param pathPrefix 이 접두어로 시작하는 경로에 적용
 * @param requiredRole 필요한 역할 (예: ROLE_ADMIN)
 */
public record RoleRule(HttpMethod method, String pathPrefix, String requiredRole) {

    // 면접방 생성은 ADMIN만. (interview-service InterviewRoomController.createInterviewRoom)
    public static final List<RoleRule> RULES = List.of(
            new RoleRule(HttpMethod.POST, "/interview/room/create", "ROLE_ADMIN")
    );

    /** 이 규칙이 주어진 요청에 적용되는가 */
    public boolean matches(HttpMethod requestMethod, String path) {
        boolean methodOk = (method == null) || method.equals(requestMethod);
        return methodOk && path.startsWith(pathPrefix);
    }
}
