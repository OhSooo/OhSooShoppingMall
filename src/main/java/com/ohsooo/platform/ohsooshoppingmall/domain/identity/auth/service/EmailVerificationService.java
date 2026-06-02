package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.request.EmailVerificationConfirmRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.request.EmailVerificationSendRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response.EmailVerificationResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response.EmailVerificationSendResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.exception.AuthErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import com.ohsooo.platform.ohsooshoppingmall.global.mail.EmailSender;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

  private static final SecureRandom R = new SecureRandom();

  private final EmailVerificationCodeStore codeStore;
  private final EmailSender emailSender;

  @Value("${app.mail.signup-code-ttl-seconds:300}")
  private long signupCodeTtlSeconds;

  @Value("${app.mail.signup-verified-ttl-seconds:1800}")
  private long signupVerifiedTtlSeconds;

  /**
   * 회원가입용 이메일 인증번호 발송
   * - 인증번호를 Redis에 저장(TTL)
   * - SMTP로 인증번호 발송
   */
  public EmailVerificationSendResponseDto sendSignupCode(EmailVerificationSendRequestDto request) {
    String email = request.getEmail();

    String code = String.format("%06d", R.nextInt(1_000_000));
    codeStore.saveSignupCode(email, code, Duration.ofSeconds(signupCodeTtlSeconds));

    emailSender.sendSignupCode(email, code);

    return new EmailVerificationSendResponseDto(email, signupCodeTtlSeconds);
  }

  /**
   * 회원가입용 이메일 인증번호 확인
   * - Redis에 저장된 인증번호와 비교
   * - 성공 시 verified 상태를 Redis에 저장(TTL)
   * - 인증번호는 1회용이므로 삭제
   */
  public EmailVerificationResponseDto confirmSignupCode(EmailVerificationConfirmRequestDto request) {
    String email = request.getEmail();
    String inputCode = request.getCode();

    String saved = codeStore.findSignupCode(email)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED));

    if (!saved.equals(inputCode)) {
      throw new BusinessException(AuthErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH);
    }

    codeStore.markSignupVerified(email, Duration.ofSeconds(signupVerifiedTtlSeconds));
    codeStore.deleteSignupCode(email);

    return new EmailVerificationResponseDto(email, true);
  }
}
