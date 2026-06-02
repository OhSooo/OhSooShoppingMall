package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.entity.AuthIdentity;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.entity.AuthProvider;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.repository.AuthIdentityRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.repository.UserRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthLoginService {

  private final UserRepository userRepository;
  private final AuthIdentityRepository authIdentityRepository;

  /**
   * 소셜 로그인 성공 시:
   * - provider + providerUserId 기준으로 AuthIdentity 조회
   * - 없으면 User 생성 후 AuthIdentity 생성
   */
  @Transactional
  public Long findOrCreateUserId(AuthProvider provider, String providerUserId) {
    return authIdentityRepository.findByProviderAndProviderUserId(provider, providerUserId)
        .map(ai -> ai.getUser().getUserId())
        .orElseGet(() -> createUserAndIdentity(provider, providerUserId));
  }

  private Long createUserAndIdentity(AuthProvider provider, String providerUserId) {
    // 온보딩에서 정보 받을 거라면, 여기서는 최소 user만 생성해도 됨
    User user = User.createForSocialOnboarding(); // <- 너 User 생성 로직에 맞게 바꿔
    User savedUser = userRepository.save(user);

    AuthIdentity authIdentity = AuthIdentity.ofSocial(savedUser, provider, providerUserId);
    authIdentityRepository.save(authIdentity);

    return savedUser.getUserId();
  }
}
