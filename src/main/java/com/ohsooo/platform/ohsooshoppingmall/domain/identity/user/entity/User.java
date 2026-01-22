package com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity;

import com.ohsooo.platform.ohsooshoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "name", length = 255, nullable = false)
  private String name;

  @Column(name = "birth", nullable = false)
  private LocalDate birth;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender", nullable = false)
  private Gender gender;

  @Column(name = "phone", length = 50, nullable = false)
  private String phone;

  @Column(name = "address", length = 255, nullable = false)
  private String address;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private Role role;

  /** 로컬 회원가입용 생성자 */
  public static User createForLocalSignup(
      String name,
      LocalDate birth,
      Gender gender,
      String phone,
      String address
  ) {
    User u = new User();
    u.name = name;
    u.birth = birth;
    u.gender = gender;
    u.phone = phone;
    u.address = address;
    u.role = Role.GENERAL;
    return u;
  }

  /** 소셜 회원가입 (온보딩 전) */
  public static User createForSocialOnboarding() {
    User u = new User();
    u.role = Role.GENERAL;
    return u;
  }

  /** 프로필 수정 (PATCH) */
  public void updateProfile(
      String name,
      LocalDate birth,
      Gender gender,
      String phone,
      String address
  ) {
    if (name != null) this.name = name;
    if (birth != null) this.birth = birth;
    if (gender != null) this.gender = gender;
    if (phone != null) this.phone = phone;
    if (address != null) this.address = address;
  }

  public void softDelete(OffsetDateTime deletedAt) {
    if (Boolean.TRUE.equals(getIsDeleted())) return;
    markDeleted(deletedAt);
  }
}
