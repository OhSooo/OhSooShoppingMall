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
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.JwtProvider;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.RefreshTokenCookieHelper;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.TokenResponse;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.TokenService;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.mapper.TokenMapper;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Auth", description = "인증/인가 API")
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

  /**
   * 로컬 회원가입
   * - 선행조건: 이메일 인증 완료(인증번호 확인 성공)
   * - 동작:
   *   1) 인증 완료 여부 검증
   *   2) 로컬 계정 생성(비밀번호 해시 포함)
   *   3) 가입 결과 반환
   */
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
   * - refresh: HttpOnly 쿠키로 내려줌
   * - access: JSON body로 내려줌
   *
   * 동작:
   *  1) 이메일/비밀번호 검증
   *  2) access/refresh 토큰 발급
   *  3) refresh 쿠키 세팅 + access 응답 바디 반환
   */
  @Operation(
      summary = "로컬 로그인",
      description = "이메일과 비밀번호로 로컬 로그인을 수행합니다. 성공 시 refresh token은 HttpOnly 쿠키로, access token은 응답 바디로 반환됩니다."
  )
  @PostMapping("/login/local")
  public ResponseEntity<BaseResponse<AccessTokenResponseDto>> loginLocal(
      @Valid @RequestBody LocalLoginRequestDto request
  ) {
    Long userId = authService.loginLocal(request);

    Map<String, Object> accessClaims = authService.buildAccessClaims(userId);
    TokenResponse tokens = tokenService.issueTokens(userId, accessClaims);

    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE,
            refreshCookieHelper.buildRefreshCookie(tokens.getRefreshToken()).toString()
        )
        .body(BaseResponse.success("로그인 성공", tokenMapper.toAccessTokenResponseDto(tokens)));
  }

  /**
   * AccessToken 재발급
   * - refresh는 쿠키에서 읽음
   * - access는 JSON body로 반환
   *
   * 주의:
   * - access 만료 상황에서도 호출되므로 @AuthenticationPrincipal 사용하면 안 됨.
   * - refresh 유효성/만료 여부를 서버에서 검증하고, 필요 시 refresh도 로테이션될 수 있음.
   */
  @Operation(
      summary = "AccessToken 재발급",
      description = "AccessToken이 만료되었을 때 refresh token(쿠키)을 이용해 access token을 재발급합니다. 필요 시 refresh token도 함께 갱신되어 쿠키로 내려갑니다."
  )
  @PostMapping("/token/reissue")
  public ResponseEntity<BaseResponse<AccessTokenResponseDto>> reissue(HttpServletRequest request) {
    String refreshToken = refreshCookieHelper.readRefreshToken(request).orElse(null);
    if (!StringUtils.hasText(refreshToken)) {
      return ResponseEntity.badRequest()
          .body(BaseResponse.error(400, "Refresh token 쿠키가 없습니다."));
    }

    if (!jwtProvider.isValid(refreshToken)) {
      return ResponseEntity.status(401)
          .body(BaseResponse.error(401, "유효하지 않은 refresh token 입니다."));
    }

    Long userId = jwtProvider.getUserId(refreshToken);

    Map<String, Object> accessClaims = authService.buildAccessClaims(userId);
    TokenResponse tokens = tokenService.reissue(userId, refreshToken, accessClaims);

    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE,
            refreshCookieHelper.buildRefreshCookie(tokens.getRefreshToken()).toString()
        )
        .body(BaseResponse.success("AccessToken 재발급 성공", tokenMapper.toAccessTokenResponseDto(tokens)));
  }

  /**
   * 로그아웃
   * - refresh를 Redis에서 삭제하여 재발급을 막음
   * - refresh 쿠키 만료 처리
   *
   * access는 서버에 저장되지 않으므로(Stateless) 서버가 직접 '삭제'할 수 없음.
   * 대신 refresh를 끊어 access 만료 이후 재인증 불가하게 만들어 실질 로그아웃을 보장함.
   */
  @Operation(
      summary = "로그아웃",
      description = "refresh token을 무효화(예: Redis 삭제)하고, refresh 쿠키를 만료시켜 로그아웃 처리합니다."
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
   * 보안(현업 권장):
   * - email 존재 여부를 응답에서 구분하지 않도록 처리할 수도 있음(계정 추측 방지)
   */
  @Operation(
      summary = "비밀번호 재발급(임시 비밀번호 발급)",
      description = "로컬 계정 이메일로 임시 비밀번호를 발급(재설정)하고, 해당 임시 비밀번호를 이메일로 발송합니다."
  )
  @PostMapping("/password/reset")
  public ResponseEntity<BaseResponse<PasswordResetResponseDto>> resetPassword(
      @Valid @RequestBody PasswordResetRequestDto request
  ) {
    authService.sendTemporaryPassword(request.getEmail());
    PasswordResetResponseDto body = authMapper.toPasswordResetResponseDto(request.getEmail());

    return ResponseEntity.ok(
        BaseResponse.success("임시 비밀번호 발급 요청이 접수되었습니다.", body)
    );
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
      description = "로그인된 사용자가 현재 비밀번호를 검증한 뒤 새 비밀번호로 변경합니다. (로컬 계정 전용)"
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

  /**
   * 이메일 인증번호 발송(회원가입)
   * - 동작:
   *   1) 이메일 유효성/중복 등 검증(서비스 정책에 따라)
   *   2) 인증번호 생성 및 저장(예: Redis)
   *   3) 이메일로 인증번호 발송
   */
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

  /**
   * 이메일 인증번호 확인(회원가입)
   * - 동작:
   *   1) 저장된 인증번호와 일치 여부 확인
   *   2) 성공 시 '인증 완료' 상태 저장(예: Redis/DB)
   */
  @Operation(
      summary = "이메일 인증번호 확인",
      description = "전송된 이메일 인증번호를 확인합니다. 성공 시 해당 이메일은 '인증 완료' 상태로 처리됩니다."
  )
  @PostMapping("/email/verification/confirm")
  public ResponseEntity<BaseResponse<EmailVerificationResponseDto>> confirmEmailVerificationCode(
      @Valid @RequestBody EmailVerificationConfirmRequestDto request
  ) {
    EmailVerificationResponseDto body = emailVerificationService.confirmSignupCode(request);
    return ResponseEntity.ok(BaseResponse.success("이메일 인증이 완료되었습니다.", body));
  }
}
