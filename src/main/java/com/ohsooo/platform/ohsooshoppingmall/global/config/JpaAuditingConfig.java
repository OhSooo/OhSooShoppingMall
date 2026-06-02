package com.ohsooo.platform.ohsooshoppingmall.global.config;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

@Configuration
public class JpaAuditingConfig {

  /**
   * JPA Auditing에서 createdAt/updatedAt을 OffsetDateTime으로 채우기 위한 Provider
   */
  @Bean(name = "offsetDateTimeProvider")
  public DateTimeProvider offsetDateTimeProvider() {
    return () -> Optional.of(OffsetDateTime.now());
  }
}
