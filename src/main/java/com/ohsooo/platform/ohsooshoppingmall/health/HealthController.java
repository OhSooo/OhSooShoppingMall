package com.ohsooo.platform.ohsooshoppingmall.health;

import com.ohsooo.platform.ohsooshoppingmall.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("ok");
    }

    @GetMapping("/health/error")
    public ApiResponse<Void> error() {
        throw new RuntimeException("test error");
    }
}