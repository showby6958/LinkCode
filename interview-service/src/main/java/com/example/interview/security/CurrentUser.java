package com.example.interview.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 붙여 게이트웨이가 넘겨준 사용자 정보를 주입받는다.
 * 기존 {@code @AuthenticationPrincipal CustomPrincipal}을 대체한다.
 *
 * <p>예: {@code public ResponseEntity<?> create(@CurrentUser LoginUser user)}
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
