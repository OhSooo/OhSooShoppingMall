package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.request;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.Gender;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로컬 회원가입 요청 DTO
 * - 이메일 인증 완료된 사용자만 가능
 * - User + AuthIdentity 생성에 필요한 모든 정보 포함
 */
@Getter
@NoArgsConstructor
public class LocalSignupRequestDto {

  @Email
  @NotBlank
  private String email;

  @NotBlank
  @Size(min = 8, max = 72)
  private String password;

  @NotBlank
  @Size(max = 255)
  private String name;

  @NotNull
  private LocalDate birth;

  @NotNull
  private Gender gender;

  @NotBlank
  @Size(max = 50)
  private String phone;

  @NotBlank
  @Size(max = 255)
  private String address;
}
