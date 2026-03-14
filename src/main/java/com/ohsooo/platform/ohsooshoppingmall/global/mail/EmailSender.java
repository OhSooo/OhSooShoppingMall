package com.ohsooo.platform.ohsooshoppingmall.global.mail;

public interface EmailSender {
  void sendSignupCode(String toEmail, String code);
  void sendTemporaryPassword(String toEmail, String tempPassword);
}
