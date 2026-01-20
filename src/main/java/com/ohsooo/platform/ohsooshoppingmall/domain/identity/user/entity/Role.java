package com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 role")
public enum Role {

  @Schema(description = "일반 회원")
  GENERAL,

  @Schema(description = "스토어 관리자")
  OWNER,

  @Schema(description = "운영자")
  ADMIN
  
}
