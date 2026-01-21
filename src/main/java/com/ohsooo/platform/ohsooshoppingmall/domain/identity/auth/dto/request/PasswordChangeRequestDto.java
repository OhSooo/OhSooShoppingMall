package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordChangeRequestDto {

  @NotBlank
  private String currentPassword;

  @NotBlank
  @Size(min = 8, max = 72)
  private String newPassword;

  @NotBlank
  private String newPasswordConfirm;

}
