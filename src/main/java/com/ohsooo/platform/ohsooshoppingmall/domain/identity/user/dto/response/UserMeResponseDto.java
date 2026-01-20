package com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.Gender;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.Role;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserMeResponseDto {

  private Long userId;
  private String name;
  private OffsetDateTime birth;
  private Gender gender;
  private String phone;
  private String address;
  private Role role;

  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
