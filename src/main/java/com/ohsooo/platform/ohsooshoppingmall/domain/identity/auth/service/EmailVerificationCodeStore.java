package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.service;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerificationCodeStore {

  private final StringRedisTemplate redis;

  public void saveSignupCode(String email, String code, Duration ttl) {
    redis.opsForValue().set(signupCodeKey(email), code, ttl);
  }

  public Optional<String> findSignupCode(String email) {
    return Optional.ofNullable(redis.opsForValue().get(signupCodeKey(email)));
  }

  public void deleteSignupCode(String email) {
    redis.delete(signupCodeKey(email));
  }

  public void markSignupVerified(String email, Duration ttl) {
    redis.opsForValue().set(signupVerifiedKey(email), "true", ttl);
  }

  public boolean isSignupVerified(String email) {
    return "true".equals(redis.opsForValue().get(signupVerifiedKey(email)));
  }

  public void clearSignupVerified(String email) {
    redis.delete(signupVerifiedKey(email));
  }

  private String signupCodeKey(String email) {
    return "email:signup:code:" + email;
  }

  private String signupVerifiedKey(String email) {
    return "email:signup:verified:" + email;
  }
}
