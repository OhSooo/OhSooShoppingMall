package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로컬 회원가입
 * - email/password는 AuthIdentity에 들어감
 * - name은 User에 들어갈 가능성이 높아서 일단 포함
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
  private String name;
}
