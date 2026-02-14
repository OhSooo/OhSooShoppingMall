package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.OptionResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.Option;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.OptionType;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.mapper.OptionMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.OptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OptionService {

    private final OptionRepository optionRepository;
    private final OptionMapper optionMapper;
    private final ItemRepository itemRepository;

    // 아이템 존재 여부 확인
    private void validateItemExists(Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new IllegalArgumentException("존재하지 않는 아이템입니다.");
        }
    }

    // 단건 조회
    @Transactional(readOnly = true)
    public OptionResponse findOptionById(Long id) {
        Option option = optionRepository.findById(id)
                .orElseThrow(()-> new IllegalArgumentException("옵션을 찾을 수 없습니다"));
        return optionMapper.toResponse(option);
    }

    // 아이템 id 기준 옵션 조회
    @Transactional(readOnly = true)
    public List<OptionResponse> findOptionsByItemId(Long itemId) {

        // 1. 아이템 존재 여부 확인
        validateItemExists(itemId);

        // 2. 옵션 조회
        return optionRepository.findByItem_ItemId(itemId)
                .stream()
                .map(optionMapper::toResponse)
                .toList();
    }

    // 아이템 + 옵션 타입 기준 조회
    @Transactional(readOnly = true)
    public List<OptionResponse> findOptionsByItemIdAndType(Long itemId, OptionType type) {

        // 1. 아이템 존재 여부 확인
        validateItemExists(itemId);

        // 2. 옵션 조회
        return optionRepository.findByItem_ItemIdAndType(itemId, type)
                .stream()
                .map(optionMapper::toResponse)
                .toList();
    }

    // 옵션 존재 여부 조회
    @Transactional(readOnly = true)
    public boolean optionExists(Long itemId, OptionType type, String value) {

        validateItemExists(itemId);

        return optionRepository.existsByItem_ItemIdAndTypeAndValue(
                itemId, type, value
        );
    }


    // 옵션 생성
    public void createOption(Long itemId, OptionType type, String value) {

        validateItemExists(itemId);

        if (optionRepository.existsByItem_ItemIdAndTypeAndValue(itemId, type, value)) {
            throw new IllegalStateException("이미 존재하는 옵션입니다.");
        }

        Option option = new Option(
                itemRepository.getReferenceById(itemId),
                type,
                value
        );

        optionRepository.save(option);
    }




}
