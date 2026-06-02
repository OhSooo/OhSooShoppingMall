package com.ohsooo.platform.ohsooshoppingmall.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    String token = resolveBearer(request);

    if (!StringUtils.hasText(token)) {
      filterChain.doFilter(request, response);
      return;
    }

    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      filterChain.doFilter(request, response);
      return;
    }

    if (!jwtProvider.isValid(token)) {
      filterChain.doFilter(request, response);
      return;
    }

    Long userId = jwtProvider.getUserId(token);

    List<GrantedAuthority> authorities = new ArrayList<>();

    String role = jwtProvider.getClaimAsString(token, "role");

    if (StringUtils.hasText(role)) {
      String normalized =
          role.startsWith("ROLE_")
              ? role
              : "ROLE_" + role;

      authorities.add(
          new SimpleGrantedAuthority(normalized)
      );
    }

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            userId,
            null,
            authorities
        );

    SecurityContextHolder.getContext()
        .setAuthentication(auth);

    filterChain.doFilter(request, response);
  }

  private String resolveBearer(HttpServletRequest request) {
    String header =
        request.getHeader(HttpHeaders.AUTHORIZATION);

    if (!StringUtils.hasText(header)) {
      return null;
    }

    if (!header.startsWith("Bearer ")) {
      return null;
    }

    return header.substring(7);
  }
}