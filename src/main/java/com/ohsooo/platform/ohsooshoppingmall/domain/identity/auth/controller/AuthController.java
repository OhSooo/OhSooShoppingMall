package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.request.LocalLoginRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.request.LocalSignupRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response.AccessTokenResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.service.AuthService;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.JwtProvider;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.RefreshTokenCookieHelper;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.TokenResponse;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.TokenService;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.mapper.TokenMapper;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;
  private final TokenService tokenService;

  private final JwtProvider jwtProvider; // refresh에서 userId 파싱용
  private final RefreshTokenCookieHelper refreshCookieHelper;

  private final TokenMapper tokenMapper;

  /**
   * 로컬 회원가입
   * - refresh: HttpOnly 쿠키
   * - access: JSON body
   */
  @PostMapping("/signup/local")
  public ResponseEntity<BaseResponse<AccessTokenResponseDto>> signupLocal(
      @Valid @RequestBody LocalSignupRequestDto request
  ) {
    Long userId = authService.signupLocal(request);

    Map<String, Object> accessClaims = authService.buildAccessClaims(userId);
    TokenResponse tokens = tokenService.issueTokens(userId, accessClaims);

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookieHelper.buildRefreshCookie(tokens.getRefreshToken()).toString())
        .body(BaseResponse.success("회원가입 성공", tokenMapper.toAccessTokenResponseDto(tokens)));
  }

  /**
   * 로컬 로그인
   * - refresh: HttpOnly 쿠키
   * - access: JSON body
   */
  @PostMapping("/login/local")
  public ResponseEntity<BaseResponse<AccessTokenResponseDto>> loginLocal(
      @Valid @RequestBody LocalLoginRequestDto request
  ) {
    Long userId = authService.loginLocal(request);

    Map<String, Object> accessClaims = authService.buildAccessClaims(userId);
    TokenResponse tokens = tokenService.issueTokens(userId, accessClaims);

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookieHelper.buildRefreshCookie(tokens.getRefreshToken()).toString())
        .body(BaseResponse.success("로그인 성공", tokenMapper.toAccessTokenResponseDto(tokens)));
  }

  /**
   * AccessToken 재발급
   * - refresh는 쿠키에서 읽음
   * - access는 JSON body로 반환
   *
   * 주의: access 만료 상황에서도 호출되니 @AuthenticationPrincipal 쓰면 안 됨.
   */
  @PostMapping("/token/reissue")
  public ResponseEntity<BaseResponse<AccessTokenResponseDto>> reissue(HttpServletRequest request) {
    String refreshToken = refreshCookieHelper.readRefreshToken(request).orElse(null);
    if (!StringUtils.hasText(refreshToken)) {
      return ResponseEntity.badRequest().body(BaseResponse.error(400, "Refresh token 쿠키가 없습니다."));
    }

    if (!jwtProvider.isValid(refreshToken)) {
      return ResponseEntity.status(401).body(BaseResponse.error(401, "유효하지 않은 refresh token 입니다."));
    }

    Long userId = jwtProvider.getUserId(refreshToken);

    Map<String, Object> accessClaims = authService.buildAccessClaims(userId);
    TokenResponse tokens = tokenService.reissue(userId, refreshToken, accessClaims);

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookieHelper.buildRefreshCookie(tokens.getRefreshToken()).toString())
        .body(BaseResponse.success("AccessToken 재발급 성공", tokenMapper.toAccessTokenResponseDto(tokens)));
  }

  /**
   * 로그아웃
   * - refresh를 Redis에서 삭제 (재발급 불가능하게)
   * - refresh 쿠키 만료
   *
   * access는 서버에 저장된 게 아니라서(Stateless) 서버가 '삭제'는 못함.
   * 대신 refresh를 끊어버리면 access 만료 이후 다시 못 살아남 = 실질 로그아웃.
   */
  @PostMapping("/logout")
  public ResponseEntity<BaseResponse<Void>> logout(HttpServletRequest request) {
    String refreshToken = refreshCookieHelper.readRefreshToken(request).orElse(null);
    if (StringUtils.hasText(refreshToken) && jwtProvider.isValid(refreshToken)) {
      Long userId = jwtProvider.getUserId(refreshToken);
      tokenService.logout(userId);
    }

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookieHelper.clearRefreshCookie().toString())
        .body(BaseResponse.success("로그아웃 완료", null));
  }
}
