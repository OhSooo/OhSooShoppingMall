package com.ohsooo.platform.ohsooshoppingmall.global.oAuth;

import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OAuthTestController {

  /* provider가 서버에 어떤 정보를 response 했는지 조회용 */
  @GetMapping("/me")
  public Map<String, Object> me(OAuth2AuthenticationToken token,
      @AuthenticationPrincipal Object principal) {
    // token.getPrincipal()이 OAuth2User/OidcUser라서 attributes 보기 편함
    return token.getPrincipal().getAttributes();
  }

  /* provider가 알려주는 유저 고유의 값을 통일해서 보여줌 (DB에 저장됨) */
  @GetMapping("/me/id")
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
      // openid 쓰면 보통 sub가 있음
      providerUserId = String.valueOf(attrs.get("sub"));
    }

    return Map.of(
        "provider", provider,
        "providerUserId", providerUserId
    );
  }

}
