package com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * PG 연동에 필요한 설정값을 관리하는 Properties.
 *
 * 사용 시점:
 * - TossPgClient에서 API 호출 시 baseUrl/secretKey를 가져오기 위해 사용
 * - TossWebhookVerifier에서 웹훅 서명 검증 시 webhookSecret을 가져오기 위해 사용(선택)
 *
 * 설정 위치 예시(application.yml):
 * pg:
 *   toss:
 *     base-url: https://api.tosspayments.com
 *     secret-key: test_sk_...
 *     webhook-secret: ...
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "pg")
public class PgProperties {

  private final Toss toss = new Toss();

  @Getter
  @Setter
  @Validated
  public static class Toss {

    /**
     * 토스 결제 API Base URL
     * 예) https://api.tosspayments.com
     */
    @NotBlank
    private String baseUrl;

    /**
     * 토스 Secret Key (승인 API 호출에 사용)
     * Basic 인증에 사용됨. (secretKey + ":" 를 Base64 인코딩)
     */
    @NotBlank
    private String secretKey;

    /**
     * (선택) 토스 웹훅 서명 검증에 쓰는 시크릿/키
     * - 검증 방식은 연동 스펙에 따라 달라서, 실제 사용 시 verifier에서 규칙에 맞게 사용
     */
    private String webhookSecret;
  }
}
