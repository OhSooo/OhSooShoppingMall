package com.ohsooo.platform.ohsooshoppingmall.domain.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderCreateRequestDto.OrderSource;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderCreateRequestDto.ShippingInfo;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.exception.OrderErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.service.OrderService;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.service.OrderValidator;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 주문 생성 시 배송 정보 필수 검증. (#71)
 * - 서버가 회원 프로필로 채우지 않고, 배송 정보가 없거나 필수값이 비어 있으면 500이 아닌 400으로 거부한다.
 * - Controller → GlobalExceptionHandler 응답 변환만 검증한다. (Spring 컨텍스트/DB/Security 없음)
 */
@ExtendWith(MockitoExtension.class)
class OrderControllerShippingTest {

  @Mock
  private OrderService orderService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(new OrderController(orderService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void 배송정보_없이_주문하면_400으로_거부하고_주문을_생성하지_않는다() throws Exception {
    mockMvc.perform(post("/orders")
            .header("Accept-Language", "ko-KR")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                { "source": "DIRECT", "items": [{ "itemVariantId": 1, "quantity": 1 }] }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("COMMON_400"))
        .andExpect(jsonPath("$.message").value("shipping: null이어서는 안됩니다"));

    verify(orderService, never()).createOrder(any(), any());
  }

  @Test
  void 배송정보의_필수값이_비어있으면_400으로_거부한다() throws Exception {
    mockMvc.perform(post("/orders")
            .header("Accept-Language", "ko-KR")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "source": "DIRECT",
                  "items": [{ "itemVariantId": 1, "quantity": 1 }],
                  "shipping": { "receiverName": "김민지", "receiverPhone": "010-2010-0001", "shippingAddress": " " }
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("COMMON_400"))
        .andExpect(jsonPath("$.message").value("shipping.shippingAddress: 공백일 수 없습니다"));

    verify(orderService, never()).createOrder(any(), any());
  }

  @Test
  void 배송정보를_모두_입력하면_주문_생성으로_넘어간다() throws Exception {
    mockMvc.perform(post("/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "source": "DIRECT",
                  "items": [{ "itemVariantId": 1, "quantity": 1 }],
                  "shipping": { "receiverName": "김민지", "receiverPhone": "010-2010-0001", "shippingAddress": "서울특별시 마포구" }
                }
                """))
        .andExpect(status().isOk());

    verify(orderService).createOrder(any(), any());
  }

  @Test
  void 서비스를_직접_호출해도_배송정보가_없으면_400_에러코드로_거부한다() {
    OrderValidator validator = new OrderValidator();
    OrderCreateRequestDto noShipping = new OrderCreateRequestDto(OrderSource.CART_ALL, null, null, null, null, null);
    OrderCreateRequestDto blankPhone = new OrderCreateRequestDto(OrderSource.CART_ALL, null, null, null,
        new ShippingInfo("김민지", "", "서울특별시 마포구", null, null, null), null);

    for (OrderCreateRequestDto request : new OrderCreateRequestDto[]{noShipping, blankPhone}) {
      assertThatThrownBy(() -> validator.validateShippingRequiredFields(request))
          .isInstanceOf(BusinessException.class)
          .extracting(e -> ((BusinessException) e).getErrorCode())
          .isEqualTo(OrderErrorCode.SHIPPING_REQUIRED_FIELDS_MISSING);
    }
    assertThat(OrderErrorCode.SHIPPING_REQUIRED_FIELDS_MISSING.getStatus().value()).isEqualTo(400);
  }
}
