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
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${app.oauth.redirect-uri}")
  private String redirectUri;

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

    TokenResponse tokens = tokenService.issueTokens(userId, authService.buildAccessClaims(userId, provider));

    response.addHeader(HttpHeaders.SET_COOKIE,
        refreshCookieHelper.buildRefreshCookie(tokens.getRefreshToken()).toString());

    response.sendRedirect(redirectUri);
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
    return String.valueOf(attrs.get("sub"));
  }
}
