package com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.dto.request.UserProfileUpdateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.dto.response.UserMeResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.service.UserService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

  private final UserService userService;

  /**
   * 내 정보 조회
   */
  @GetMapping("/me")
  public ResponseEntity<BaseResponse<UserMeResponseDto>> getMe(@AuthenticationPrincipal Long userId) {
    UserMeResponseDto response = userService.getMe(userId);
    return ResponseEntity.ok(BaseResponse.success("내 정보 조회 성공", response));
  }

  /**
   * 내 프로필 수정
   * - 온보딩(추가정보 입력)과 마이페이지 수정 모두 이 엔드포인트로 처리 가능
   */
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
   */
  @DeleteMapping("/me")
  public ResponseEntity<BaseResponse<Void>> deleteMe(@AuthenticationPrincipal Long userId) {
    userService.softDelete(userId);
    return ResponseEntity.ok(BaseResponse.success("회원 탈퇴가 완료되었습니다.", null));
  }
}
