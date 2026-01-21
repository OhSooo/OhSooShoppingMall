package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response.AuthIdentityResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response.LocalSignupResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.entity.AuthIdentity;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

  public AuthIdentityResponseDto toAuthIdentityResponseDto(AuthIdentity entity) {
    return new AuthIdentityResponseDto(
        entity.getId(),
        entity.getUser().getUserId(),
        entity.getProvider(),
        entity.getProviderUserId(),
        entity.getEmail(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public LocalSignupResponseDto toLocalSignupResponseDto(AuthIdentity entity) {
    return new LocalSignupResponseDto(
        entity.getUser().getUserId(),
        entity.getId(),
        entity.getEmail()
    );
  }
}
