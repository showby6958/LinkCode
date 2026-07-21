package com.example.interview.security;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@code @CurrentUser LoginUser} 파라미터에 X-User-* 헤더에서 만든 LoginUser를 넣어준다.
 *
 * <p>Spring Security의 SecurityContext를 쓰지 않으므로, interview-service는 보안
 * 의존성 없이도 사용자 정보를 컨트롤러로 전달할 수 있다. 헤더가 없으면 null이 들어가고,
 * 접근 제어(인증·역할)는 이미 게이트웨이가 마쳤다.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(LoginUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        return HeaderUserCodec.from(
                webRequest.getHeader(HeaderUserCodec.H_USER_ID),
                webRequest.getHeader(HeaderUserCodec.H_EMAIL),
                webRequest.getHeader(HeaderUserCodec.H_NAME),
                webRequest.getHeader(HeaderUserCodec.H_PICTURE),
                webRequest.getHeader(HeaderUserCodec.H_ROLE)
        );
    }
}
