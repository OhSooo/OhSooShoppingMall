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

        JwtAuthenticationFilter jwtAuthenticationFilter =
            new JwtAuthenticationFilter(jwtProvider);

        http
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())

            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            )

            .authorizeHttpRequests(auth -> auth

                // health
                .requestMatchers("/health/**").permitAll()

                // swagger
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // oauth
                .requestMatchers(
                    "/",
                    "/auth/**",
                    "/oauth2/**",
                    "/login/**"
                ).permitAll()

                // auth
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

                // user
                .requestMatchers("/users/me/**")
                .authenticated()

                // cart
                .requestMatchers("/cart/**")
                .authenticated()

                // order
                .requestMatchers("/orders/**")
                .authenticated()

                // payment
                .requestMatchers("/payments/webhook/**")
                .permitAll()

                .requestMatchers("/payments/**")
                .authenticated()

                // inventory
                .requestMatchers(HttpMethod.GET, "/inventory/**")
                .permitAll()

                .requestMatchers(HttpMethod.PATCH, "/inventory/**")
                .hasAnyRole("OWNER", "ADMIN")

                // catalog
                .requestMatchers(HttpMethod.GET, "/catalog/item/**")
                .permitAll()

                .requestMatchers(HttpMethod.GET, "/catalog/category/**")
                .permitAll()

                .requestMatchers(HttpMethod.GET, "/catalog/itemVariant/**")
                .permitAll()

                .requestMatchers(HttpMethod.GET, "/catalog/item-variant-option/**")
                .permitAll()

                .requestMatchers(HttpMethod.GET, "/catalog/option/**")
                .permitAll()

                // store
                .requestMatchers(HttpMethod.GET, "/stores")
                .permitAll()

                .requestMatchers(HttpMethod.GET, "/stores/*")
                .permitAll()

                // OWNER
                .requestMatchers(HttpMethod.POST, "/stores")
                .hasRole("OWNER")

                .requestMatchers(HttpMethod.PATCH, "/stores/*/status")
                .hasRole("OWNER")

                .requestMatchers(HttpMethod.POST, "/catalog/option")
                .hasRole("OWNER")

                // ADMIN
                .requestMatchers(HttpMethod.PATCH, "/admin/stores/*/status")
                .hasRole("ADMIN")

                .anyRequest().authenticated()
            )

            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            .oauth2Login(oauth2 ->
                oauth2.successHandler(oAuth2SuccessHandler));

        return http.build();
    }

}
