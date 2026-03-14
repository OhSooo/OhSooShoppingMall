package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response;

import java.time.OffsetDateTime;

public class PasswordChangeResponseDto {

  private boolean changed;
  private OffsetDateTime changedAt;

  protected PasswordChangeResponseDto() {}

  public PasswordChangeResponseDto(boolean changed, OffsetDateTime changedAt) {
    this.changed = changed;
    this.changedAt = changedAt;
  }

  public boolean isChanged() {
    return changed;
  }

  public OffsetDateTime getChangedAt() {
    return changedAt;
  }
}
