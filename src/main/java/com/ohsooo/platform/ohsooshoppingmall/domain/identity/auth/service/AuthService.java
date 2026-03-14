package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.request.LocalLoginRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.request.LocalSignupRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.request.PasswordChangeRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.entity.AuthIdentity;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.entity.AuthProvider;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.exception.AuthErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.repository.AuthIdentityRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.repository.UserRepository;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import com.ohsooo.platform.ohsooshoppingmall.global.mail.EmailSender;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final AuthIdentityRepository authIdentityRepository;
  private final PasswordEncoder passwordEncoder;

  private final EmailSender emailSender;

  private final EmailVerificationCodeStore emailVerificationCodeStore;

  /**
   * 로컬 회원가입
   * - 이메일 인증 여부 확인
   * - users 생성 (모든 필드 포함)
   * - auth_identities(LOCAL) 생성
   * - 이메일 인증 verified 상태 제거 (1회용)
   *
   * @return 생성된 AuthIdentity
   */
  @Transactional
  public AuthIdentity signupLocal(LocalSignupRequestDto request) {

    if (!emailVerificationCodeStore.isSignupVerified(request.getEmail())) {
      throw new BusinessException(AuthErrorCode.EMAIL_NOT_VERIFIED);
    }

    AuthIdentity result = authIdentityRepository
        .findByProviderAndEmail(AuthProvider.LOCAL, request.getEmail())
        .map(existing -> {
          User user = existing.getUser();

          // 이미 활성 계정이면 중복
          if (!Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
          }

          // 탈퇴 계정이면 "복구"
          user.restore(); // is_deleted=false, deleted_at=null
          user.updateProfile(
              request.getName(),
              request.getBirth(),
              request.getGender(),
              request.getPhone(),
              request.getAddress()
          );
          user.markOnboarded();

          // auth_identity도 soft delete 상태일 수 있으니 복구 (선택)
          if (Boolean.TRUE.equals(existing.getIsDeleted())) {
            existing.restore();
          }

          // 비밀번호/이메일 업데이트
          existing.changePasswordHash(passwordEncoder.encode(request.getPassword()));
          existing.updateEmail(request.getEmail());

          return existing;
        })
        .orElseGet(() -> {
          // 신규 생성
          User user = User.createForLocalSignup(
              request.getName(),
              request.getBirth(),
              request.getGender(),
              request.getPhone(),
              request.getAddress()
          );
          User savedUser = userRepository.save(user);

          AuthIdentity auth = AuthIdentity.ofLocal(
              savedUser,
              passwordEncoder.encode(request.getPassword()),
              request.getEmail()
          );
          return authIdentityRepository.save(auth);
        });

    emailVerificationCodeStore.clearSignupVerified(request.getEmail());

    return result;
  }




  /**
   * 로컬 로그인
   * - auth_identities(LOCAL,email) 조회
   * - 비밀번호 검증
   * - user soft delete 여부 확인
   * @return userId
   */
  @Transactional(readOnly = true)
  public Long loginLocal(LocalLoginRequestDto request) {
    AuthIdentity auth = authIdentityRepository
        .findByProviderAndEmail(AuthProvider.LOCAL, request.getEmail())
        .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

    User user = auth.getUser();
    if (Boolean.TRUE.equals(user.getIsDeleted())) {
      throw new BusinessException(AuthErrorCode.USER_DELETED);
    }

    String hash = auth.getPasswordHash();
    if (hash == null || !passwordEncoder.matches(request.getPassword(), hash)) {
      throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
    }

    return user.getUserId();
  }

  /**
   * 소셜 로그인 성공 시
   * - provider + providerUserId 로 AuthIdentity 조회
   * - 없으면 User 생성 + AuthIdentity 생성
   */
  @Transactional
  public Long findOrCreateSocialUserId(AuthProvider provider, String providerUserId) {
    return authIdentityRepository.findByProviderAndProviderUserId(provider, providerUserId)
        .map(ai -> {
          User user = ai.getUser();
          if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new BusinessException(AuthErrorCode.USER_DELETED);
          }
          return user.getUserId();
        })
        .orElseGet(() -> createSocialUserAndIdentity(provider, providerUserId));
  }

  private Long createSocialUserAndIdentity(AuthProvider provider, String providerUserId) {
    User user = User.createForSocialOnboarding();
    User savedUser = userRepository.save(user);

    AuthIdentity auth = AuthIdentity.ofSocial(savedUser, provider, providerUserId);
    authIdentityRepository.save(auth);

    return savedUser.getUserId();
  }

  /**
   * AccessToken에 넣을 claims 생성
   */
  @Transactional(readOnly = true)
  public Map<String, Object> buildAccessClaims(Long userId) {
    User user = userRepository.findByUserIdAndIsDeletedFalse(userId)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

    Map<String, Object> claims = new HashMap<>();
    claims.put("role", user.getRole().name());
    return claims;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> buildAccessClaims(Long userId, AuthProvider provider) {
    Map<String, Object> claims = buildAccessClaims(userId);
    claims.put("provider", provider.name());
    return claims;
  }

  /**
   * 비밀번호 재발급(임시 비밀번호 발송)
   * - 대상: 로컬 계정(LOCAL)
   * - 동작:
   *   1) auth_identities(LOCAL, email) 조회
   *   2) 임시 비밀번호 생성
   *   3) password_hash를 임시 비밀번호 해시로 교체
   *   4) 이메일로 임시 비밀번호 발송
   */
  @Transactional
  public void sendTemporaryPassword(String email) {
    AuthIdentity auth = authIdentityRepository
        .findByProviderAndEmail(AuthProvider.LOCAL, email)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_IDENTITY_NOT_FOUND));

    if (auth.getPasswordHash() == null) {
      throw new BusinessException(AuthErrorCode.SOCIAL_PASSWORD_NOT_SUPPORTED);
    }

    String tempPassword = generateTempPassword(12);
    auth.changePasswordHash(passwordEncoder.encode(tempPassword));

    emailSender.sendTemporaryPassword(email, tempPassword);
  }

  /**
   * 비밀번호 변경(마이페이지)
   * - 인증: AccessToken 필요(@AuthenticationPrincipal userId)
   * - 대상: 로컬 계정(LOCAL)
   * - 동작:
   *   1) 새 비밀번호/확인 일치 검증
   *   2) auth_identities(userId, LOCAL) 조회
   *   3) 현재 비밀번호 검증
   *   4) password_hash 갱신
   *
   * @return 변경 시각
   */
  @Transactional
  public OffsetDateTime changePassword(Long userId, PasswordChangeRequestDto request) {
    if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
      throw new BusinessException(AuthErrorCode.NEW_PASSWORD_CONFIRM_MISMATCH);
    }

    AuthIdentity auth = authIdentityRepository
        .findByUser_UserIdAndProvider(userId, AuthProvider.LOCAL)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_IDENTITY_NOT_FOUND));

    String hash = auth.getPasswordHash();
    if (hash == null) {
      throw new BusinessException(AuthErrorCode.SOCIAL_PASSWORD_NOT_SUPPORTED);
    }

    if (!passwordEncoder.matches(request.getCurrentPassword(), hash)) {
      throw new BusinessException(AuthErrorCode.PASSWORD_MISMATCH);
    }

    auth.changePasswordHash(passwordEncoder.encode(request.getNewPassword()));

    return OffsetDateTime.now();
  }

  /**
   * 임시 비밀번호 생성
   * - 영문 대/소 + 숫자
   * - 혼동되는 문자(0/O, 1/I/l 등)는 제외
   */
  private String generateTempPassword(int length) {
    String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    SecureRandom r = new SecureRandom();

    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(chars.charAt(r.nextInt(chars.length())));
    }
    return sb.toString();
  }
}
