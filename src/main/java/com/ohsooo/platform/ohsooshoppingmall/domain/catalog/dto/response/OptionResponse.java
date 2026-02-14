package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Item;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.OptionType;
import lombok.Getter;

@Getter
public class OptionResponse {

    private Long optionId;

    // Item 요약 정보
    private final Long itemId;
    private final String itemName;

    private OptionType type;
    private String value;

    public OptionResponse(
            Long optionId,
            Long itemId,
            String itemName,
            OptionType type,
            String value
    ) {
        this.optionId = optionId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.type = type;
        this.value = value;
    }
}
