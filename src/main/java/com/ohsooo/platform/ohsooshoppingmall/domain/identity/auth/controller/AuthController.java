package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.request.EmailVerificationConfirmRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.request.EmailVerificationSendRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.request.LocalLoginRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.request.LocalSignupRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.request.PasswordChangeRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.request.PasswordResetRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response.AccessTokenResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response.EmailVerificationResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response.EmailVerificationSendResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response.LocalSignupResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response.PasswordChangeResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.response.PasswordResetResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.entity.AuthIdentity;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.mapper.AuthMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.service.AuthService;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.service.EmailVerificationService;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.JwtProvider;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.RefreshTokenCookieHelper;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.TokenResponse;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.TokenService;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.mapper.TokenMapper;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;
  private final TokenService tokenService;

  private final EmailVerificationService emailVerificationService;

  private final JwtProvider jwtProvider; // refresh에서 userId 파싱용
  private final RefreshTokenCookieHelper refreshCookieHelper;

  private final TokenMapper tokenMapper;

  private final AuthMapper authMapper;

  @Operation(
      summary = "로컬 회원가입",
      description = "이메일 인증이 완료된 사용자의 로컬 회원가입을 처리합니다."
  )
  @PostMapping("/signup/local")
  public ResponseEntity<BaseResponse<LocalSignupResponseDto>> signupLocal(
      @Valid @RequestBody LocalSignupRequestDto request
  ) {
    AuthIdentity auth = authService.signupLocal(request);
    LocalSignupResponseDto body = authMapper.toLocalSignupResponseDto(auth);

    return ResponseEntity.ok(
        BaseResponse.success("회원가입이 완료되었습니다.", body)
    );
  }


  /**
   * 로컬 로그인
   * - refresh: HttpOnly 쿠키
   * - access: JSON body
   */
  @Operation(
      summary = "로컬 로그인",
      description = "이메일과 비밀번호로 로컬 로그인을 수행합니다."
  )
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
  @Operation(
      summary = "AccessToken 재발급",
      description = "AccessToken이 만료되면 refresh Token을 보고 재발급 해줍니다."
  )
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
  @Operation(
      summary = "로그아웃",
      description = "refresh Token을 무효화 시킵니다."
  )
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

  /**
   * 비밀번호 재발급(임시 비밀번호 발송)
   * - 대상: 로컬 로그인 사용자(LOCAL)
   * - 동작:
   *   1) auth_identities(LOCAL, email) 조회
   *   2) 임시 비밀번호 생성 및 password_hash 교체
   *   3) 이메일 발송
   *
   * - 보안(현업 권장):
   *   * email 존재 여부를 응답에서 구분하지 않도록 처리 가능(계정 추측 방지)
   */
  @Operation(
      summary = "비밀번호 재발급(임시 비밀번호 발급)",
      description = "이메일로 임시 비밀번호를 발송합니다."
  )
  @PostMapping("/password/reset")
  public ResponseEntity<BaseResponse<PasswordResetResponseDto>> resetPassword(
      @Valid @RequestBody PasswordResetRequestDto request
  ) {
    authService.sendTemporaryPassword(request.getEmail());
    PasswordResetResponseDto body = authMapper.toPasswordResetResponseDto(request.getEmail());

    return ResponseEntity.ok(BaseResponse.success("임시 비밀번호 발급 요청이 접수되었습니다.", body));
  }

  /**
   * 비밀번호 변경(마이페이지)
   * - 인증: AccessToken 필요(@AuthenticationPrincipal userId)
   * - 대상: 로컬 로그인 사용자(LOCAL)
   * - 동작:
   *   1) 현재 비밀번호 검증
   *   2) 새 비밀번호/확인 일치 검증
   *   3) password_hash 갱신
   */
  @Operation(
      summary = "비밀번호 변경",
      description = "로그인된 사용자가 비밀번호를 변경합니다."
  )
  @PatchMapping("/password")
  public ResponseEntity<BaseResponse<PasswordChangeResponseDto>> changePassword(
      @AuthenticationPrincipal Long userId,
      @Valid @RequestBody PasswordChangeRequestDto request
  ) {
    OffsetDateTime changedAt = authService.changePassword(userId, request);
    PasswordChangeResponseDto body = authMapper.toPasswordChangeResponseDto(changedAt);

    return ResponseEntity.ok(BaseResponse.success("비밀번호 변경 완료", body));
  }



  @Operation(
      summary = "이메일 인증번호 발송",
      description = "회원가입을 위한 이메일 인증번호를 발송합니다."
  )
  @PostMapping("/email/verification/send")
  public ResponseEntity<BaseResponse<EmailVerificationSendResponseDto>> sendEmailVerificationCode(
      @Valid @RequestBody EmailVerificationSendRequestDto request
  ) {
    EmailVerificationSendResponseDto body = emailVerificationService.sendSignupCode(request);
    return ResponseEntity.ok(BaseResponse.success("인증번호가 발송되었습니다.", body));
  }

  @Operation(
      summary = "이메일 인증번호 확인",
      description = "전송된 이메일 인증번호를 확인합니다."
  )
  @PostMapping("/email/verification/confirm")
  public ResponseEntity<BaseResponse<EmailVerificationResponseDto>> confirmEmailVerificationCode(
      @Valid @RequestBody EmailVerificationConfirmRequestDto request
  ) {
    EmailVerificationResponseDto body = emailVerificationService.confirmSignupCode(request);
    return ResponseEntity.ok(BaseResponse.success("이메일 인증이 완료되었습니다.", body));
  }

}
