package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationResponseDto {

  private String email;
  private boolean verified;
}
