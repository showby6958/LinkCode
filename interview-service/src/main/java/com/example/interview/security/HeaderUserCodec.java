package com.example.interview.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 게이트웨이가 넣어준 X-User-* 헤더를 {@link LoginUser}로 바꾼다.
 * REST(ArgumentResolver)와 WebSocket(핸드셰이크 인터셉터)이 공유한다.
 *
 * <p>값은 게이트웨이가 Base64-URL로 인코딩해 보낸다. 한글 이름이 HTTP 헤더(ASCII)에서
 * 깨지는 것을 막기 위함이다.
 */
public final class HeaderUserCodec {

    public static final String H_USER_ID = "X-User-Id";
    public static final String H_EMAIL = "X-User-Email";
    public static final String H_NAME = "X-User-Name";
    public static final String H_PICTURE = "X-User-Picture";
    public static final String H_ROLE = "X-User-Role";

    private HeaderUserCodec() {
    }

    /**
     * 다섯 헤더 값(모두 Base64-URL 인코딩됨)으로 LoginUser를 만든다.
     * userId가 비어 있으면 = 게이트웨이가 유효 토큰을 못 찾은 요청 → null.
     */
    public static LoginUser from(String rawUserId, String rawEmail, String rawName,
                                 String rawPicture, String rawRole) {
        if (rawUserId == null || rawUserId.isBlank()) {
            return null;
        }
        return new LoginUser(
                Long.valueOf(decode(rawUserId)),
                decode(rawEmail),
                decode(rawName),
                decode(rawPicture),
                decode(rawRole)
        );
    }

    private static String decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
