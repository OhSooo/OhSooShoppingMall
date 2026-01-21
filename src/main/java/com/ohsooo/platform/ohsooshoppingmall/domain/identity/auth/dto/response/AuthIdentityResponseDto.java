package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.entity.AuthProvider;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthIdentityResponseDto {

  private Long authIdentityId;
  private Long userId;
  private AuthProvider provider;
  private String providerUserId;
  private String email;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
