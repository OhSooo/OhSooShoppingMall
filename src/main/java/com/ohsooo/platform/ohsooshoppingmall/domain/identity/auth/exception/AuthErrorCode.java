package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.exception;

import com.ohsooo.platform.ohsooshoppingmall.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

  AUTH_IDENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_4041", "인증 정보가 존재하지 않습니다."),
  EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_4091", "이미 가입된 이메일입니다."),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_4011", "이메일 또는 비밀번호가 올바르지 않습니다."),
  USER_DELETED(HttpStatus.FORBIDDEN, "AUTH_4031", "탈퇴한 사용자입니다."),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_4042", "사용자를 찾을 수 없습니다."),

  PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH_4012", "현재 비밀번호가 올바르지 않습니다."),
  NEW_PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_4001", "새 비밀번호 확인이 일치하지 않습니다."),
  SOCIAL_PASSWORD_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "AUTH_4002", "소셜 계정은 비밀번호를 변경할 수 없습니다."),

  EMAIL_VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_4003", "인증번호가 만료되었습니다."),
  EMAIL_VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_4004", "인증번호가 올바르지 않습니다."),
  EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "AUTH_4005", "이메일 인증이 필요합니다."),

  INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_4013", "유효하지 않은 리프레시 토큰입니다."),
  REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_4014", "리프레시 토큰이 존재하지 않습니다."),
  REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH_4015", "리프레시 토큰이 일치하지 않습니다.");


  private final HttpStatus status;
  private final String code;
  private final String message;

}
