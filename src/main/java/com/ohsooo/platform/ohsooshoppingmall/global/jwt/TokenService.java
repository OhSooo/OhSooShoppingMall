package com.ohsooo.platform.ohsooshoppingmall.global.jwt;

import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

  private final JwtProvider jwtProvider;
  private final JwtProperties props;
  private final RefreshTokenStore refreshTokenStore;

  /* AccessToken 발급 */
  public TokenResponse issueTokens(Long userId, Map<String, Object> accessClaims) {
    String access = jwtProvider.createAccessToken(userId, accessClaims);
    String refresh = jwtProvider.createRefreshToken(userId);

    refreshTokenStore.save(userId, refresh, Duration.ofSeconds(props.getRefreshTokenSeconds()));

    return TokenResponse.builder()
        .accessToken(access)
        .refreshToken(refresh)
        .tokenType("Bearer")
        .expiresIn(props.getAccessTokenSeconds())
        .build();
  }

  /* AccessToken 재발급 */
  public TokenResponse reissue(Long userId, String refreshToken, Map<String, Object> accessClaims) {
    if (!jwtProvider.isValid(refreshToken)) {
      throw new IllegalArgumentException("Invalid refresh token");
    }

    String saved = refreshTokenStore.find(userId)
        .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

    if (!saved.equals(refreshToken)) {
      throw new IllegalArgumentException("Refresh token mismatch");
    }

    return issueTokens(userId, accessClaims);
  }

  /* 로그아웃 */
  public void logout(Long userId) {
    refreshTokenStore.delete(userId);
  }
}
