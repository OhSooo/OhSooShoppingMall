package com.ohsooo.platform.ohsooshoppingmall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class OhSooShoppingMallApplication {

	public static void main(String[] args) {
		SpringApplication.run(OhSooShoppingMallApplication.class, args);
	}

}
