package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.OptionResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Item;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Option;
import org.springframework.stereotype.Component;

@Component
public class OptionMapper {

    public OptionResponse toResponse(final Option option) {

        Item item = option.getItem();

        return new OptionResponse(
                option.getOptionId(),
                item.getItemId(),
                item.getName(),
                option.getType(),
                option.getValue()
        );
    }
}
