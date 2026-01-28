package com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.repository.AuthIdentityRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.dto.request.UserProfileUpdateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.dto.response.UserMeResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.exception.UserErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.mapper.UserMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.repository.UserRepository;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final AuthIdentityRepository authIdentityRepository;

  public UserMeResponseDto getMe(Long userId) {
    validateAuthPrincipal(userId);

    User user = userRepository.findByUserIdAndIsDeletedFalse(userId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    return userMapper.toMeResponseDto(user);
  }

  @Transactional
  public void updateMe(Long userId, UserProfileUpdateRequestDto request) {
    validateAuthPrincipal(userId);

    User user = userRepository.findByUserIdAndIsDeletedFalse(userId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    user.updateProfile(
        request.getName(),
        request.getBirth(),
        request.getGender(),
        request.getPhone(),
        request.getAddress(),
        request.getShippingPostcode(),
        request.getShippingAddressDetail()
    );

  }

  @Transactional
  public void softDelete(Long userId) {
    validateAuthPrincipal(userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    if (Boolean.TRUE.equals(user.getIsDeleted())) {
      throw new BusinessException(UserErrorCode.USER_DELETED);
    }

    OffsetDateTime now = OffsetDateTime.now();
    user.softDelete(now);

    authIdentityRepository.findAllByUser_UserId(userId)
        .forEach(ai -> ai.softDelete(now));   // AuthIdentity에 softDelete 추가했을 때
  }


  private void validateAuthPrincipal(Long userId) {
    if (userId == null) {
      throw new BusinessException(UserErrorCode.AUTH_PRINCIPAL_MISSING);
    }
  }
}
