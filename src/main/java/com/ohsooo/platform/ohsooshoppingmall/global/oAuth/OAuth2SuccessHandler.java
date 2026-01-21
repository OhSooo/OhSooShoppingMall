package com.ohsooo.platform.ohsooshoppingmall.global.oAuth;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.entity.AuthProvider;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.service.AuthService;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.RefreshTokenCookieHelper;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.TokenResponse;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.TokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

  private final AuthService authService;
  private final TokenService tokenService;
  private final RefreshTokenCookieHelper refreshCookieHelper;

  /**
   * 소셜 로그인 성공
   * - DB: (provider, providerUserId)로 AuthIdentity 조회/없으면 생성
   * - 서버: refresh를 Redis에 저장
   * - 응답: refresh_token HttpOnly 쿠키만 세팅
   * - 그리고 프론트 URL로 redirect
   *
   * 프론트는 redirect 후 /auth/token/reissue 호출해서 access(JSON) 받으면 됨.
   */
  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication
  ) throws IOException, ServletException {

    OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;

    String registrationId = token.getAuthorizedClientRegistrationId(); // google/naver/kakao
    AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase()); // GOOGLE/NAVER/KAKAO

    Map<String, Object> attrs = token.getPrincipal().getAttributes();
    String providerUserId = extractProviderUserId(registrationId, attrs);

    Long userId = authService.findOrCreateSocialUserId(provider, providerUserId);

    // refresh 저장(Redis) + access/refresh 발급(여기서 access는 사용 안 해도 됨)
    TokenResponse tokens = tokenService.issueTokens(userId, authService.buildAccessClaims(userId, provider));

    // refresh 쿠키만 세팅
    response.addHeader(HttpHeaders.SET_COOKIE,
        refreshCookieHelper.buildRefreshCookie(tokens.getRefreshToken()).toString());


    // 프론트로 redirect (예: http://localhost:5173/oauth/callback)
    response.sendRedirect("http://localhost:5173/oauth/callback");
  }

  @SuppressWarnings("unchecked")
  private String extractProviderUserId(String provider, Map<String, Object> attrs) {
    if ("naver".equals(provider)) {
      Map<String, Object> res = (Map<String, Object>) attrs.get("response");
      return String.valueOf(res.get("id"));
    }
    if ("kakao".equals(provider)) {
      return String.valueOf(attrs.get("id"));
    }
    // google (openid scope면 보통 sub)
    return String.valueOf(attrs.get("sub"));
  }
}
