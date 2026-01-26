package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.OptionResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Option;
import org.springframework.stereotype.Component;

@Component
public class OptionMapper {

    public OptionResponse toResponse(final Option option) {
        return new OptionResponse(
                option.getOptionId(),
                option.getItem(),
                option.getType(),
                option.getValue()
        );
    }
}
