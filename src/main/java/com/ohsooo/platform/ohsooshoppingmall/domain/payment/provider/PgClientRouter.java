package com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.PaymentProvider;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.exception.PaymentErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.client.PgClient;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * provider(enum)에 따라 실제 PgClient 구현체를 라우팅하는 클래스.
 *
 * 사용 시점:
 * - Service 레이어에서 payment.provider 값에 따라 올바른 PG client를 가져오기 위해 사용
 *
 * 현재:
 * - TOSS만 매핑
 * 확장:
 * - KAKAOPAY, NICE 등 추가 시 Map에 등록
 */
@Component
@RequiredArgsConstructor
public class PgClientRouter {

  private final com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.client.TossPgClient tossPgClient;

  public PgClient route(PaymentProvider provider) {
    if (provider == null) {
      throw new BusinessException(PaymentErrorCode.INVALID_PROVIDER);
    }
    return switch (provider) {
      case TOSS -> tossPgClient;
      default -> throw new BusinessException(PaymentErrorCode.INVALID_PROVIDER);
    };
  }
}
