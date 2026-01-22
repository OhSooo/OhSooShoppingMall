package com.ohsooo.platform.ohsooshoppingmall.global.oAuth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "OAuth Test", description = "소셜 로그인 연동 테스트용 API (개발/디버깅 목적)")
@RestController
public class OAuthTestController {

  /**
   * provider가 서버에 어떤 정보를 response 했는지 조회용
   * - token.getPrincipal()이 OAuth2User/OidcUser 이므로 attributes 확인에 유리
   * - 운영 환경에서는 민감정보 노출 위험이 있으니 보호/제거 권장
   */
  @Operation(
      summary = "소셜 로그인 사용자 attributes 조회",
      description = "OAuth2 로그인 이후 Provider(google/naver/kakao)가 내려준 attributes 원본을 그대로 반환합니다. (디버깅 목적)"
  )
  @GetMapping("/me")
  public Map<String, Object> me(
      OAuth2AuthenticationToken token,
      @AuthenticationPrincipal Object principal
  ) {
    return token.getPrincipal().getAttributes();
  }

  /**
   * provider가 알려주는 유저 고유의 값을 통일해서 보여줌 (DB에 저장됨)
   * - google: 보통 sub (OpenID Connect)
   * - naver: response.id
   * - kakao: id
   *
   * 주의:
   * - 각 provider의 응답 스키마는 설정(스코프/사용 라이브러리)에 따라 달라질 수 있음.
   * - 디버깅/테스트 용도로만 사용 권장.
   */
  @Operation(
      summary = "소셜 로그인 providerUserId 통일 조회",
      description = "Provider별 고유 식별자(providerUserId)를 통일된 형태로 추출해 반환합니다."
  )
  @GetMapping("/me/id")
  @SuppressWarnings("unchecked")
  public Map<String, Object> meId(OAuth2AuthenticationToken token) {
    String provider = token.getAuthorizedClientRegistrationId(); // google/naver/kakao
    Map<String, Object> attrs = token.getPrincipal().getAttributes();

    String providerUserId;
    if ("naver".equals(provider)) {
      Map<String, Object> response = (Map<String, Object>) attrs.get("response");
      providerUserId = String.valueOf(response.get("id"));
    } else if ("kakao".equals(provider)) {
      providerUserId = String.valueOf(attrs.get("id"));
    } else { // google
      providerUserId = String.valueOf(attrs.get("sub"));
    }

    return Map.of(
        "provider", provider,
        "providerUserId", providerUserId
    );
  }
}
