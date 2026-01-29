package com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * PG 연동용 HTTP Client 설정.
 *
 * 사용 시점:
 * - TossPgClient가 Toss API(승인/취소/조회 등)를 호출할 때 사용
 *
 * 구현 포인트:
 * - Spring Boot 3.x / Spring 6의 RestClient 사용
 * - baseUrl은 PgProperties에서 주입
 */
@Configuration
@EnableConfigurationProperties(PgProperties.class)
public class PgClientConfig {

  @Bean
  public RestClient tossRestClient(PgProperties pgProperties) {
    return RestClient.builder()
        .baseUrl(pgProperties.getToss().getBaseUrl())
        .build();
  }
}
