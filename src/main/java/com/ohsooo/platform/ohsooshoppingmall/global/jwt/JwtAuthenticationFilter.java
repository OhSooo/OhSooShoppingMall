package com.ohsooo.platform.ohsooshoppingmall.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
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

    // 토큰이 없으면 그냥 통과
    if (!StringUtils.hasText(token)) {
      filterChain.doFilter(request, response);
      return;
    }

    // 이미 인증 정보가 있으면 중복 세팅 방지
    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      filterChain.doFilter(request, response);
      return;
    }

    // 유효하지 않으면 그냥 통과(보통 401/403은 Security 규칙에서 처리)
    if (!jwtProvider.isValid(token)) {
      filterChain.doFilter(request, response);
      return;
    }

    Long userId = jwtProvider.getUserId(token);
    if (token != null && jwtProvider.isValid(token)) {
      Long userId = jwtProvider.getUserId(token);
      String role = jwtProvider.getRole(token);

    // role 클레임을 authorities로 주입 (hasRole/hasAnyRole과 호환되게 ROLE_ prefix 필수)
    List<GrantedAuthority> authorities = new ArrayList<>();
    String role = jwtProvider.getClaimAsString(token, "role"); // 예: "OWNER", "ADMIN", "GENERAL"

    if (StringUtils.hasText(role)) {
      String normalized = role.startsWith("ROLE_") ? role : "ROLE_" + role;
      authorities.add(new SimpleGrantedAuthority(normalized));
    }

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(userId, null, authorities);
      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));

    SecurityContextHolder.getContext().setAuthentication(auth);

    filterChain.doFilter(request, response);
  }

  private String resolveBearer(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (!StringUtils.hasText(header)) return null;
    if (!header.startsWith("Bearer ")) return null;
    return header.substring(7);
  }
}
