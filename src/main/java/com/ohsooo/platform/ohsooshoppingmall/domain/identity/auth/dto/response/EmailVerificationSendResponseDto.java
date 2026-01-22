package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationSendResponseDto {
  private String email;
  private long expiresInSeconds; // 인증번호 유효시간(초)
}
