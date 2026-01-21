package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.request.LocalLoginRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.dto.request.LocalSignupRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.entity.AuthIdentity;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.entity.AuthProvider;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.exception.AuthErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.repository.AuthIdentityRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.repository.UserRepository;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
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

  /**
   * 로컬 회원가입
   * - users 생성
   * - auth_identities(LOCAL) 생성
   * @return userId
   */
  @Transactional
  public Long signupLocal(LocalSignupRequestDto request) {
    // 1) 로컬 이메일 중복 체크
    authIdentityRepository.findByProviderAndEmail(AuthProvider.LOCAL, request.getEmail())
        .ifPresent(x -> { throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS); });

    // 2) User 생성
    User user = User.createForLocalSignup(request.getName()); // Role.GENERAL 세팅됨
    User savedUser = userRepository.save(user);

    // 3) AuthIdentity(LOCAL) 생성
    String passwordHash = passwordEncoder.encode(request.getPassword());
    AuthIdentity auth = AuthIdentity.ofLocal(savedUser, passwordHash, request.getEmail());
    authIdentityRepository.save(auth);

    return savedUser.getUserId();
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
    // 소셜은 온보딩에서 정보 채울 거니까 role만 세팅된 유저 생성
    User user = User.createForSocialOnboarding(); // Role.GENERAL 세팅됨
    User savedUser = userRepository.save(user);

    AuthIdentity auth = AuthIdentity.ofSocial(savedUser, provider, providerUserId);
    authIdentityRepository.save(auth);

    return savedUser.getUserId();
  }

  /**
   * AccessToken에 넣을 claims 생성
   * - 최소: role
   * - 권장: provider(프론트/서버 디버깅에 도움)
   */
  @Transactional(readOnly = true)
  public Map<String, Object> buildAccessClaims(Long userId) {
    User user = userRepository.findByUserIdAndIsDeletedFalse(userId)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

    Map<String, Object> claims = new HashMap<>();
    claims.put("role", user.getRole().name());
    return claims;
  }

  /**
   * provider까지 claim에 포함하고 싶을 때 사용
   * - 소셜 로그인 성공 시에 많이 씀
   */
  @Transactional(readOnly = true)
  public Map<String, Object> buildAccessClaims(Long userId, AuthProvider provider) {
    Map<String, Object> claims = buildAccessClaims(userId);
    claims.put("provider", provider.name());
    return claims;
  }
}
