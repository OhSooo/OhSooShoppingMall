package com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Toss 웹훅 payload 파싱용 DTO.
 *
 * Toss가 보내는 웹훅 payload 예시:
 * {
 *   "eventType": "PAYMENT_STATUS_CHANGED",
 *   "createdAt": "2024-01-01T00:00:00+09:00",
 *   "data": {
 *     "paymentKey": "...",
 *     "orderId": "...",
 *     "status": "DONE",
 *     ...
 *   }
 * }
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossWebhookPayload {

  private String eventType;
  private TossWebhookData data;

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TossWebhookData {

    private String paymentKey;
    private String status;
  }
}
