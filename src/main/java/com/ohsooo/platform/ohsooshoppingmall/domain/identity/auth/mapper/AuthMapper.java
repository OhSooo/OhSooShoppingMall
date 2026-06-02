package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response.AuthIdentityResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response.LocalSignupResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response.PasswordChangeResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response.PasswordResetResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.entity.AuthIdentity;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

  /**
   * AuthIdentity 엔티티 → 조회용 응답 DTO 변환
   */
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

  /**
   * 로컬 회원가입 응답 DTO 변환
   */
  public LocalSignupResponseDto toLocalSignupResponseDto(AuthIdentity entity) {
    return new LocalSignupResponseDto(entity.getUser().getName());
  }

  /**
   * 비밀번호 재발급(임시비밀번호 발송) 응답 DTO 변환
   * - 보안 정책에 따라 accepted는 항상 true로 내려주는 형태를 권장
   */
  public PasswordResetResponseDto toPasswordResetResponseDto(String email) {
    return new PasswordResetResponseDto(true, email);
  }

  /**
   * 비밀번호 변경 응답 DTO 변환
   */
  public PasswordChangeResponseDto toPasswordChangeResponseDto(OffsetDateTime changedAt) {
    return new PasswordChangeResponseDto(true, changedAt);
  }
}
