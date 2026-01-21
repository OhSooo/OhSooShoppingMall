package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.entity;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import com.ohsooo.platform.ohsooshoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "auth_identities",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_auth_user_provider", columnNames = {"user_id", "provider"}),
        @UniqueConstraint(name = "uk_auth_provider_user_id", columnNames = {"provider", "provider_user_id"})
    }
)
public class AuthIdentity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "auth_identities_id", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_auth_identity_user"))
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 20)
  private AuthProvider provider;

  @Column(name = "provider_user_id", length = 255)
  private String providerUserId;

  @Column(name = "password_hash", length = 255)
  private String passwordHash;

  @Column(name = "email", length = 255)
  private String email;

  private AuthIdentity(User user, AuthProvider provider, String providerUserId, String passwordHash, String email) {
    this.user = user;
    this.provider = provider;
    this.providerUserId = providerUserId;
    this.passwordHash = passwordHash;
    this.email = email;
  }

  public static AuthIdentity ofSocial(User user, AuthProvider provider, String providerUserId) {
    return new AuthIdentity(user, provider, providerUserId, null, null);
  }

  public static AuthIdentity ofLocal(User user, String passwordHash, String email) {
    return new AuthIdentity(user, AuthProvider.LOCAL, null, passwordHash, email);
  }

  public void changePasswordHash(String newPasswordHash) {
    this.passwordHash = newPasswordHash;
  }

  public void updateEmail(String email) {
    this.email = email;
  }
}
