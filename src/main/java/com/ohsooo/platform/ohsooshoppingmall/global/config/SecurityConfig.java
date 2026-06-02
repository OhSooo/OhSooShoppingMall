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
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())

        // JWT 필터: UsernamePasswordAuthenticationFilter 앞
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
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

            // ===== 공개 GET =====
            .requestMatchers(HttpMethod.GET, "/catalog/item/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/catalog/category/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/catalog/itemVariant/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/catalog/item-variant-option/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/catalog/option/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/stores").permitAll()
            .requestMatchers(HttpMethod.GET, "/stores/*").permitAll()

            // ===== OWNER =====
            .requestMatchers(HttpMethod.POST, "/stores").hasRole("OWNER")
            .requestMatchers(HttpMethod.PATCH, "/stores/*/status").hasRole("OWNER")
            .requestMatchers(HttpMethod.POST, "/catalog/option").hasRole("OWNER")

            // ===== ADMIN =====
            .requestMatchers(HttpMethod.PATCH, "/admin/stores/*/status").hasRole("ADMIN")

            // 나머지
            .anyRequest().permitAll()
        )
        .formLogin(form -> form.disable())
        .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2SuccessHandler));



            .requestMatchers(
                "/auth/signup/local",
                "/auth/login/local",
                "/auth/token/reissue",
                "/auth/email/verification/send",
                "/auth/email/verification/confirm",
                "/auth/password/reset"
            ).permitAll()


            .requestMatchers(
                "/auth/logout",
                "/auth/password"
            ).authenticated()

            .requestMatchers("/users/me/**").authenticated()


            .requestMatchers("/cart/**").authenticated()


            .requestMatchers("/orders/**").authenticated()


            .requestMatchers("/payments/webhook/**").permitAll()
            .requestMatchers("/payments/**").authenticated()


            .requestMatchers(HttpMethod.GET, "/inventory/**").permitAll()
            .requestMatchers(HttpMethod.PATCH, "/inventory/**").hasAnyRole("OWNER", "ADMIN")


            .anyRequest().permitAll()
        )

        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())

        .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2SuccessHandler));

        return http.build();
    }
}
