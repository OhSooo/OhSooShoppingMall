package com.ohsooo.platform.ohsooshoppingmall.global.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieHelper {

  public static final String REFRESH_COOKIE_NAME = "refresh_token";

  private final JwtProperties jwtProperties;
  private final CookieProperties cookieProperties;

  /** refresh 토큰을 HttpOnly 쿠키로 내려줄 때 사용 */
  public ResponseCookie buildRefreshCookie(String refreshToken) {
    ResponseCookie.ResponseCookieBuilder builder =
        ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
            .httpOnly(true)
            .secure(cookieProperties.isSecure())
            .path(cookieProperties.getPath())
            .sameSite(cookieProperties.getSameSite())
            .maxAge(Duration.ofSeconds(jwtProperties.getRefreshTokenSeconds()));

    if (StringUtils.hasText(cookieProperties.getDomain())) {
      builder.domain(cookieProperties.getDomain());
    }

    return builder.build();
  }

  /** 로그아웃 시 쿠키 만료 */
  public ResponseCookie clearRefreshCookie() {
    ResponseCookie.ResponseCookieBuilder builder =
        ResponseCookie.from(REFRESH_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(cookieProperties.isSecure())
            .path(cookieProperties.getPath())
            .sameSite(cookieProperties.getSameSite())
            .maxAge(0);

    if (StringUtils.hasText(cookieProperties.getDomain())) {
      builder.domain(cookieProperties.getDomain());
    }

    return builder.build();
  }

  /** 요청 쿠키에서 refresh_token 읽기 */
  public Optional<String> readRefreshToken(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) return Optional.empty();

    for (Cookie c : cookies) {
      if (REFRESH_COOKIE_NAME.equals(c.getName()) && StringUtils.hasText(c.getValue())) {
        return Optional.of(c.getValue());
      }
    }
    return Optional.empty();
  }
}
