package com.ohsooo.platform.ohsooshoppingmall.global.config;

import com.ohsooo.platform.ohsooshoppingmall.global.jwt.JwtAuthenticationFilter;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.JwtProvider;
import com.ohsooo.platform.ohsooshoppingmall.global.oAuth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private final OAuth2SuccessHandler oAuth2SuccessHandler;
  private final JwtProvider jwtProvider;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtProvider);

    http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())

        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

        .authorizeHttpRequests(auth -> auth

            // swagger / openapi
            .requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html"
            ).permitAll()

            // auth / oauth
            .requestMatchers(
                "/",
                "/auth/**",
                "/oauth2/**",
                "/login/**"
            ).permitAll()

            // user
            .requestMatchers("/users/me/**").authenticated()

            .anyRequest().permitAll()
        )
        .formLogin(form -> form.disable())
        .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2SuccessHandler));

    return http.build();
  }
}
