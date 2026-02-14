package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemVariantOptionResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.mapper.ItemVariantOptionMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemVariantOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ItemVariantOptionService {

    private final ItemVariantOptionRepository itemVariantOptionRepository;
    private final ItemVariantOptionMapper itemVariantOptionMapper;

    // 특정 variant에 포함된 옵션들 조회
    @Transactional(readOnly = true)
    public List<ItemVariantOptionResponse> findOptionsByVariantId(Long itemVariantId) {

        return itemVariantOptionRepository
                .findByItemVariant_ItemVariantId(itemVariantId)
                .stream()
                .map(itemVariantOptionMapper::toResponse)
                .toList();
    }

    // 특정 variant + 특정 option 존재 여부
    @Transactional(readOnly = true)
    public boolean existsVariantOption(Long itemVariantId, Long optionId) {

        return itemVariantOptionRepository
                .existsByItemVariant_ItemVariantIdAndOption_OptionId(
                        itemVariantId, optionId
                );
    }

    // 특정 option이 사용된 variant 조회(관리 / 분석용)
    @Transactional(readOnly = true)
    public List<ItemVariantOptionResponse> findVariantsByOptionId(Long optionId) {

        return itemVariantOptionRepository
                .findByOption_OptionId(optionId)
                .stream()
                .map(itemVariantOptionMapper::toResponse)
                .toList();
    }
}
