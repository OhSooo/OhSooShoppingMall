package com.ohsooo.platform.ohsooshoppingmall.global.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

  private final JavaMailSender mailSender;

  @Value("${app.mail.from}")
  private String from;

  @Override
  public void sendSignupCode(String toEmail, String code) {
    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setFrom(from);
    msg.setTo(toEmail);
    msg.setSubject("[OhSooShoppingMall] 이메일 인증번호");
    msg.setText(
        "아래 인증번호를 입력해 주세요.\n\n" +
            "인증번호: " + code + "\n\n" +
            "유효시간: 5분\n" +
            "본 메일은 발신 전용입니다."
    );
    mailSender.send(msg);
  }

  @Override
  public void sendTemporaryPassword(String toEmail, String tempPassword) {
    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setFrom(from);
    msg.setTo(toEmail);
    msg.setSubject("[OhSooShoppingMall] 임시 비밀번호 안내");
    msg.setText(
        "임시 비밀번호가 발급되었습니다.\n\n" +
            "임시 비밀번호: " + tempPassword + "\n\n" +
            "로그인 후 반드시 비밀번호를 변경해 주세요.\n" +
            "본 메일은 발신 전용입니다."
    );
    mailSender.send(msg);
  }
}
