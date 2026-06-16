package com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.webhook;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.exception.PaymentErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.config.PgProperties;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Toss 웹훅 서명 검증기.
 *
 * 검증 방식: HMAC-SHA256(rawBody, webhookSecret) → Base64 인코딩 후 헤더 값과 비교
 * 헤더명: TossPayments-Signature
 */
@Component
@RequiredArgsConstructor
public class TossWebhookVerifier implements WebhookVerifier {

  private static final String SIGNATURE_HEADER = "TossPayments-Signature";
  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final PgProperties pgProperties;

  @Override
  public void verify(HttpServletRequest request, String rawBody) {
    String secret = pgProperties.getToss().getWebhookSecret();
    if (secret == null || secret.isBlank()) {
      throw new BusinessException(PaymentErrorCode.PG_SIGNATURE_INVALID);
    }

    String receivedSignature = request.getHeader(SIGNATURE_HEADER);
    if (receivedSignature == null || receivedSignature.isBlank()) {
      throw new BusinessException(PaymentErrorCode.PG_SIGNATURE_INVALID);
    }

    String computedSignature = computeHmacSha256(rawBody, secret);

    // timing-safe 비교로 timing attack 방지
    if (!MessageDigest.isEqual(
        computedSignature.getBytes(StandardCharsets.UTF_8),
        receivedSignature.getBytes(StandardCharsets.UTF_8))) {
      throw new BusinessException(PaymentErrorCode.PG_SIGNATURE_INVALID);
    }
  }

  private String computeHmacSha256(String data, String secret) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hmacBytes);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new BusinessException(PaymentErrorCode.PG_SIGNATURE_INVALID, e);
    }
  }
}
