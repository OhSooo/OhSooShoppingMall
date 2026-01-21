package com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.exception;

import com.ohsooo.platform.ohsooshoppingmall.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_4041", "존재하지 않는 사용자입니다."),
  USER_DELETED(HttpStatus.FORBIDDEN, "USER_4031", "탈퇴한 사용자입니다."),
  AUTH_PRINCIPAL_MISSING(HttpStatus.UNAUTHORIZED, "USER_4011", "인증 정보가 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

}
