package com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.dto.response.UserMeResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public UserMeResponseDto toMeResponseDto(User user) {
    return UserMeResponseDto.builder()
        .userId(user.getUserId())
        .name(user.getName())
        .birth(user.getBirth())
        .gender(user.getGender())
        .phone(user.getPhone())
        .address(user.getAddress())
        .role(user.getRole())
        .onboarded(user.isOnboarded())
        .build();
  }
}
