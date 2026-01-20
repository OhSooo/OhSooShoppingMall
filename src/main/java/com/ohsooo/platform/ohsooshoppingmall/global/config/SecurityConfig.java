package com.ohsooo.platform.ohsooshoppingmall.global.config;

import com.ohsooo.platform.ohsooshoppingmall.global.oAuth.OAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth

                // swagger / openapi
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // oauth2 관련
                .requestMatchers(
                    "/", "/oauth2/**", "/login/**"
                ).permitAll()

                // 테스트용
                .requestMatchers("/me").authenticated()

                .anyRequest().permitAll()
            )
            .formLogin(form -> form.disable())
            .oauth2Login(oauth2 ->
                oauth2.successHandler(oAuth2SuccessHandler)
            );

        return http.build();
    }
}