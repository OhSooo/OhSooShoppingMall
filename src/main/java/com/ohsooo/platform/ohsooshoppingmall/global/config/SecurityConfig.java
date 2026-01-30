package com.ohsooo.platform.ohsooshoppingmall.global.config;

import com.ohsooo.platform.ohsooshoppingmall.global.jwt.JwtAuthenticationFilter;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.JwtProvider;
import com.ohsooo.platform.ohsooshoppingmall.global.oAuth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity // (선택) 추후 @PreAuthorize 쓰고 싶을 때 대비
public class SecurityConfig {

  private final OAuth2SuccessHandler oAuth2SuccessHandler;
  private final JwtProvider jwtProvider;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtProvider);

    http
        // REST API + JWT 기반이면 보통 stateless
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())

        // JWT 필터: UsernamePasswordAuthenticationFilter 앞
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

        .authorizeHttpRequests(auth -> auth

            // =========================
            // 0) Swagger / OpenAPI
            // =========================
            .requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html"
            ).permitAll()

            // =========================
            // 1) OAuth2 로그인 흐름
            // =========================
            .requestMatchers(
                "/",
                "/oauth2/**",
                "/login/**"
            ).permitAll()

            // =========================
            // 2) Auth 공개 API (로그인 없이 가능)
            // =========================
            .requestMatchers(
                "/auth/signup/local",
                "/auth/login/local",
                "/auth/token/reissue",
                "/auth/email/verification/send",
                "/auth/email/verification/confirm",
                "/auth/password/reset"
            ).permitAll()

            // =========================
            // 3) Auth 중 로그인 필요 API
            // =========================
            .requestMatchers(
                "/auth/logout",
                "/auth/password"
            ).authenticated()

            // =========================
            // 4) Users (내 정보)
            // =========================
            .requestMatchers("/users/me/**").authenticated()

            // =========================
            // 5) Cart
            // =========================
            .requestMatchers("/cart/**").authenticated()

            // =========================
            // 6) Orders
            // =========================
            .requestMatchers("/orders/**").authenticated()

            // =========================
            // 7) Payments / Refunds
            // - 웹훅은 외부 PG가 호출 -> permitAll
            // =========================
            .requestMatchers("/payments/webhook/**").permitAll()
            .requestMatchers("/payments/**").authenticated()

            // =========================
            // 8) Inventory
            // - 조회(GET)는 공개(원하면 authenticated로 바꿔도 됨)
            // - 변경(PATCH)는 OWNER/ADMIN
            // =========================
            .requestMatchers(HttpMethod.GET, "/inventory/**").permitAll()
            .requestMatchers(HttpMethod.PATCH, "/inventory/**").hasAnyRole("OWNER", "ADMIN")

            // =========================
            // 9) 나머지
            // =========================
            .anyRequest().permitAll()
        )

        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())

        .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2SuccessHandler));

    return http.build();
  }
}
