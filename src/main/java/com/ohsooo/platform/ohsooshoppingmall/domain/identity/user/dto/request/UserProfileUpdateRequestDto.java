package com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.dto.request;


import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.Gender;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserProfileUpdateRequestDto {

  @Size(max = 255)
  private String name;

  private LocalDate birth;

  private Gender gender;

  @Size(max = 50)
  private String phone;

  @Size(max = 255)
  private String address;

  @Size(max = 20)
  private String shippingPostcode;

  @Size(max = 255)
  private String shippingAddressDetail;

}
