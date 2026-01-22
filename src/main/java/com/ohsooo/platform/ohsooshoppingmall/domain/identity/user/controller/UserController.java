package com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.dto.request.UserProfileUpdateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.dto.response.UserMeResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.service.UserService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "유저(내 정보) API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

  private final UserService userService;

  /**
   * 내 정보 조회
   * - 인증 필요: AccessToken
   * - 반환: 로그인한 사용자의 기본 프로필/계정 정보를 반환
   */
  @Operation(
      summary = "내 정보 조회",
      description = "로그인한 사용자의 내 정보(프로필)를 조회합니다."
  )
  @GetMapping("/me")
  public ResponseEntity<BaseResponse<UserMeResponseDto>> getMe(@AuthenticationPrincipal Long userId) {
    UserMeResponseDto response = userService.getMe(userId);
    return ResponseEntity.ok(BaseResponse.success("내 정보 조회 성공", response));
  }

  /**
   * 내 프로필 수정
   * - 인증 필요: AccessToken
   * - 온보딩(추가정보 입력)과 마이페이지 수정 모두 이 엔드포인트로 처리 가능
   * - 부분 수정(PATCH): 전달된 필드만 반영되도록 서비스에서 처리(정책에 따라)
   */
  @Operation(
      summary = "내 프로필 수정",
      description = "로그인한 사용자의 프로필 정보를 수정합니다. (온보딩/마이페이지 공용)"
  )
  @PatchMapping("/me")
  public ResponseEntity<BaseResponse<Void>> updateMe(
      @AuthenticationPrincipal Long userId,
      @Valid @RequestBody UserProfileUpdateRequestDto request
  ) {
    userService.updateMe(userId, request);
    return ResponseEntity.ok(BaseResponse.success("내 정보 수정 완료", null));
  }

  /**
   * 회원 탈퇴(soft delete)
   * - 인증 필요: AccessToken
   * - 유저 데이터를 즉시 물리 삭제하지 않고, is_deleted/deleted_at 등을 이용해 논리 삭제 처리
   */
  @Operation(
      summary = "회원 탈퇴",
      description = "로그인한 사용자를 소프트 삭제(탈퇴) 처리합니다."
  )
  @DeleteMapping("/me")
  public ResponseEntity<BaseResponse<Void>> deleteMe(@AuthenticationPrincipal Long userId) {
    userService.softDelete(userId);
    return ResponseEntity.ok(BaseResponse.success("회원 탈퇴가 완료되었습니다.", null));
  }
}
