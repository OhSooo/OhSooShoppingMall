package com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 gender")
public enum Gender {

  @Schema(description = "남성")
  MALE,

  @Schema(description = "여성")
  FEMALE,

  @Schema(description = "기타")
  OTHER

}
