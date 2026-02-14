package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.OptionType;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ItemVariantOptionResponse {

    private Long id;

    private Long optionId;
    private OptionType optionType;
    private String optionValue;

    private Long itemVariantId;
}
