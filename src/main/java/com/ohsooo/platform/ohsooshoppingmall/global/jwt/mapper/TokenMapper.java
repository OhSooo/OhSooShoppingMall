package com.ohsooo.platform.ohsooshoppingmall.global.jwt.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response.AccessTokenResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response.TokenPairResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.TokenResponse;
import org.springframework.stereotype.Component;

@Component
public class TokenMapper {

  public TokenPairResponseDto toTokenPairResponseDto(TokenResponse token) {
    return new TokenPairResponseDto(
        token.getAccessToken(),
        token.getRefreshToken()
    );
  }

  public AccessTokenResponseDto toAccessTokenResponseDto(TokenResponse token) {
    return new AccessTokenResponseDto(
        token.getAccessToken(),
        token.getTokenType(),
        token.getExpiresIn()
    );
  }
}
