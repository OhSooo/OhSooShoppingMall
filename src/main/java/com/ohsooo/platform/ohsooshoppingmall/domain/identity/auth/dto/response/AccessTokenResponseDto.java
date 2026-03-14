package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AccessToken 응답 DTO (RefreshToken은 HttpOnly 쿠키로 내려감)")
public class AccessTokenResponseDto {

  @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiJ9...")
  private String accessToken;

  @Schema(description = "토큰 타입", example = "Bearer")
  private String tokenType;

  @Schema(description = "AccessToken 만료(초)", example = "1800")
  private long expiresIn;
}
