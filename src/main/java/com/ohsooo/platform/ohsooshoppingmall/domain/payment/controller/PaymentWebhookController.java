package com.ohsooo.platform.ohsooshoppingmall.domain.payment.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.service.WebhookService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment Webhook", description = "PG 웹훅 수신 API (선택)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/payments/webhook")
public class PaymentWebhookController {

  private final WebhookService webhookService;

  @Operation(
      summary = "토스 웹훅 수신",
      description = """
          PG가 서버로 보내는 웹훅을 수신합니다. (배포 후 HTTPS 환경에서 사용 권장)
          
          - 로컬 개발 단계에서는 웹훅 없이도 confirm 기반으로 결제 기능 구현 가능
          - 웹훅은 분쟁/보험/비동기 정합성 보강용
          """
  )
  @PostMapping("/toss")
  public ResponseEntity<BaseResponse<Void>> tossWebhook(HttpServletRequest request) throws Exception {
    String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
    webhookService.handleTossWebhook(request, body);
    return ResponseEntity.ok(BaseResponse.success("웹훅 처리 성공", null));
  }
}
