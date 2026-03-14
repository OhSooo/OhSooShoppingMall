package com.ohsooo.platform.ohsooshoppingmall.global.exception;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {
  HttpStatus getStatus();
  String getCode();
  String getMessage();
}
