package com.ohsooo.platform.ohsooshoppingmall.global.jwt;

import lombok.*;

@Getter
@Builder
public class TokenResponse {
  private String accessToken;
  private String refreshToken;
  private String tokenType;
  private long expiresIn;
}
