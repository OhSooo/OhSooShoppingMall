package com.ohsooo.platform.ohsooshoppingmall.domain.payment.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.request.RefundCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.request.RefundItemRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.response.RefundDetailResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.dto.response.RefundResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Payment;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.Refund;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.RefundItem;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RefundMapper {

  /** 환불 엔티티 생성(REQUESTED) */
  public Refund toRefundEntity(Payment payment, RefundCreateRequestDto request) {
    if (payment == null || request == null) return null;
    return Refund.requested(payment, request.getAmount(), request.getReason());
  }

  /** 환불 품목 엔티티 리스트 생성 */
  public List<RefundItem> toRefundItemEntities(Refund refund, RefundCreateRequestDto request) {
    List<RefundItem> result = new ArrayList<>();
    if (refund == null || request == null || request.getItems() == null) return result;

    for (RefundItemRequestDto item : request.getItems()) {
      if (item == null) continue;
      result.add(RefundItem.of(refund, item.getOrderItemId(), item.getAmount(), item.getQuantity()));
    }
    return result;
  }

  /** 환불 요약 응답 DTO */
  public RefundResponseDto toRefundResponseDto(Refund refund) {
    if (refund == null) return null;

    return new RefundResponseDto(
        refund.getRefundId(),
        refund.getPayment() != null ? refund.getPayment().getPaymentId() : null,
        refund.getStatus().name(),
        refund.getAmount(),
        refund.getReason(),
        refund.getCreatedAt(),
        refund.getRefundedAt()
    );
  }

  /** 환불 상세 응답 DTO (품목 포함) */
  public RefundDetailResponseDto toRefundDetailResponseDto(Refund refund, List<RefundItem> items) {
    if (refund == null) return null;

    List<RefundDetailResponseDto.RefundItem> itemDtos = new ArrayList<>();
    if (items != null) {
      for (RefundItem ri : items) {
        if (ri == null) continue;
        itemDtos.add(new RefundDetailResponseDto.RefundItem(
            ri.getRefundItemId(),
            ri.getOrderItemId(),
            ri.getAmount(),
            ri.getQuantity()
        ));
      }
    }

    return new RefundDetailResponseDto(
        refund.getRefundId(),
        refund.getPayment() != null ? refund.getPayment().getPaymentId() : null,
        refund.getStatus().name(),
        refund.getAmount(),
        refund.getReason(),
        refund.getCreatedAt(),
        refund.getRefundedAt(),
        itemDtos
    );
  }
}
