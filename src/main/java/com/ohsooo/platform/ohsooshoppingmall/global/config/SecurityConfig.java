package com.ohsooo.platform.ohsooshoppingmall.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth

                // 로그인 시작/콜백 등 OAuth2 관련 경로는 열어둠
                .requestMatchers("/", "/oauth2/**", "/login/**").permitAll()


                .requestMatchers("/me").authenticated()

                .anyRequest().permitAll()
            )
            .formLogin(form -> form.disable())
            .oauth2Login(Customizer.withDefaults());

        return http.build();
    }
}