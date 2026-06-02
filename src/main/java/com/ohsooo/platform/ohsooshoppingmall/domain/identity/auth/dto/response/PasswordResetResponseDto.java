package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response;

public class PasswordResetResponseDto {

  private boolean accepted;
  private String email;

  protected PasswordResetResponseDto() {}

  public PasswordResetResponseDto(boolean accepted, String email) {
    this.accepted = accepted;
    this.email = email;
  }

  public boolean isAccepted() {
    return accepted;
  }

  public String getEmail() {
    return email;
  }
}
