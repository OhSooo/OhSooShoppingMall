package com.ohsooo.platform.ohsooshoppingmall.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

  private final JwtProperties props;
  private final Key key;

  public JwtProvider(JwtProperties props) {
    this.props = props;

    byte[] keyBytes = Decoders.BASE64.decode(props.getSecret());
    this.key = Keys.hmacShaKeyFor(keyBytes);
  }

  /* AccessToken 생성 */
  public String createAccessToken(Long userId, Map<String, Object> claims) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(props.getAccessTokenSeconds());

    // 요청 처리에 필요한 최소 정보: userId, claims(role,provider), iat, exp, signature
    return Jwts.builder()
        .setSubject(String.valueOf(userId))
        .addClaims(claims)
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(exp))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  /* RefreshToken 생성 */
  public String createRefreshToken(Long userId) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(props.getRefreshTokenSeconds());

    // 재발급용 식별자: userId, iat, exp, signature
    return Jwts.builder()
        .setSubject(String.valueOf(userId))
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(exp))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  /* 토큰을 검증하고, 그 안에 있는 payload를 추출 */
  public Jws<Claims> parse(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token);
  }

  /* 토큰에 저장된 userId 추출 */
  public Long getUserId(String token) {
    Claims claims = parse(token).getBody();
    return Long.valueOf(claims.getSubject());
  }

  /* 토큰에 저장된 role 추출*/
  public String getRole(String token) {
    Claims claims = parse(token).getBody();
    return claims.get("role", String.class);
  }

  /* 토큰의 유효성 확인 */
  public boolean isValid(String token) {
    try {
      parse(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }
}
