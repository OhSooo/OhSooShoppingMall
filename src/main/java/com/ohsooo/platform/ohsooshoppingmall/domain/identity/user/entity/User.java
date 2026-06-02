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

  // 온보딩 전엔 비어있을 수 있음
  @Column(name = "name", length = 255, nullable = true)
  private String name;

  @Column(name = "birth", nullable = true)
  private LocalDate birth;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender", nullable = true)
  private Gender gender;

  @Column(name = "phone", length = 50, nullable = true)
  private String phone;

  @Column(name = "address", length = 255, nullable = true)
  private String address;

  @Column(name = "shipping_postcode", length = 20, nullable = true)
  private String shippingPostcode;

  @Column(name = "shipping_address_detail", length = 255, nullable = true)
  private String shippingAddressDetail;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private Role role;

  @Column(name = "onboarded", nullable = false)
  private boolean onboarded;

  /** 로컬 회원가입용 생성자 (온보딩 완료 상태로 생성) */
  public static User createForLocalSignup(
      String name,
      LocalDate birth,
      Gender gender,
      String phone,
      String address,
      String shippingPostcode,
      String shippingAddressDetail
  ) {
    User u = new User();
    u.name = name;
    u.birth = birth;
    u.gender = gender;
    u.phone = phone;
    u.address = address;
    u.shippingPostcode = shippingPostcode;
    u.shippingAddressDetail = shippingAddressDetail;
    u.role = Role.GENERAL;
    u.onboarded = true;
    return u;
  }

  /** 소셜 회원가입 (온보딩 전) */
  public static User createForSocialOnboarding() {
    User u = new User();
    u.role = Role.GENERAL;
    u.onboarded = false;
    return u;
  }

  /** 프로필 수정 (PATCH) */
  public void updateProfile(
      String name,
      LocalDate birth,
      Gender gender,
      String phone,
      String address,
      String shippingPostcode,
      String shippingAddressDetail
  ) {
    if (name != null) this.name = name;
    if (birth != null) this.birth = birth;
    if (gender != null) this.gender = gender;
    if (phone != null) this.phone = phone;
    if (address != null) this.address = address;
    if (shippingPostcode != null) this.shippingPostcode = shippingPostcode;
    if (shippingAddressDetail != null) this.shippingAddressDetail = shippingAddressDetail;

    // onboarded 여부 판단
    if (this.name != null && this.birth != null && this.gender != null
        && this.phone != null && this.address != null
        && this.shippingPostcode != null && this.shippingAddressDetail != null) {
      this.onboarded = true;
    }
  }

  public void markOnboarded() {
    this.onboarded = true;
  }

  public void softDelete(OffsetDateTime deletedAt) {
    if (Boolean.TRUE.equals(getIsDeleted())) return;
    markDeleted(deletedAt);
  }

  public void restore() {
    markRestored();
  }

}
