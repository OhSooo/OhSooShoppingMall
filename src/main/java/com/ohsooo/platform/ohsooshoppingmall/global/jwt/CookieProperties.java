package com.ohsooo.platform.ohsooshoppingmall.global.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.cookie")
public class CookieProperties {

  /** 운영(HTTPS) = true, 로컬(http)= false */
  private boolean secure = false;

  /**
   * Lax: 같은 사이트 이동 중심(로컬 편함)
   * None: 크로스 사이트 쿠키 필요(프론트/백 도메인 분리 시 운영에서 거의 필수)
   */
  private String sameSite = "Lax";

  /** 쿠키 도메인. 로컬은 보통 null, 운영은 ".ohsooo.com" 같은 값 가능 */
  private String domain;

  /** refresh 쿠키 path */
  private String path = "/";
}
