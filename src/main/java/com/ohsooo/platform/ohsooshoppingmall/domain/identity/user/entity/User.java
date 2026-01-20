package com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity;

import com.ohsooo.platform.ohsooshoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "name", length = 255)
  private String name;

  @Column(name = "birth")
  private LocalDate birth;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender")
  private Gender gender;

  @Column(name = "phone", length = 50)
  private String phone;

  @Column(name = "address", length = 255)
  private String address;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private Role role;

  /**
   * 프로필(온보딩/마이페이지) 수정
   * - PATCH 성격이라 null 값은 무시
   */
  public void updateProfile(String name,
      LocalDate birth,
      Gender gender,
      String phone,
      String address) {
    if (name != null) this.name = name;
    if (birth != null) this.birth = birth;
    if (gender != null) this.gender = gender;
    if (phone != null) this.phone = phone;
    if (address != null) this.address = address;
  }

  /**
   * 회원 탈퇴(soft delete)
   * - 이미 삭제된 경우 중복 호출은 무시(원하면 예외로 바꿔도 됨)
   */
  public void softDelete(OffsetDateTime deletedAt) {
    if (Boolean.TRUE.equals(getIsDeleted())) return;
    markDeleted(deletedAt);
  }
}
