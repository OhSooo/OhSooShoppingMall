package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.entity.AuthIdentity;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.entity.AuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthIdentityRepository extends JpaRepository<AuthIdentity, Long> {

  Optional<AuthIdentity> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

  Optional<AuthIdentity> findByUser_IdAndProvider(Long userId, AuthProvider provider);

  Optional<AuthIdentity> findByProviderAndEmail(AuthProvider provider, String email);

  Optional<AuthIdentity> findByEmailAndProvider(String email, AuthProvider provider);

}
