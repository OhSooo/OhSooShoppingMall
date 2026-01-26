package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Item;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.OptionType;
import lombok.Getter;

@Getter
public class OptionResponse {

    private Long optionId;
    private Item item;
    private OptionType type;
    private String value;

    public OptionResponse(Long optionId, Item item, OptionType type, String value) {
        this.optionId = optionId;
        this.item = item;
        this.type = type;
        this.value = value;
    }
}
