package com.ohsooo.platform.ohsooshoppingmall.domain.payment.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Payment;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.PaymentEvent;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums.PaymentEventType;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.repository.PaymentEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentEventService {

  private final PaymentEventRepository paymentEventRepository;

  public void saveEvent(Payment payment, PaymentEventType type, String payloadJson) {
    PaymentEvent event = PaymentEvent.of(payment, type, payloadJson);
    paymentEventRepository.save(event);
  }
}
