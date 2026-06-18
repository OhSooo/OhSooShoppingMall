package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request.AddVariantsRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request.ItemCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request.ItemCreateRequestDto.OptionDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request.ItemCreateRequestDto.VariantDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.AddVariantsResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemCreateResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemCreateResponseDto.VariantResult;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Category;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Item;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.Option;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.OptionType;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantOption;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.exception.CatalogErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.exception.CategoryErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.exception.ItemErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.mapper.ItemMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.CategoryRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemVariantOptionRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemVariantRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.OptionRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.service.InventoryService;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.Store;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.exception.StoreErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.repository.StoreRepository;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final OptionRepository optionRepository;
    private final ItemVariantRepository itemVariantRepository;
    private final ItemVariantOptionRepository itemVariantOptionRepository;
    private final InventoryService inventoryService;

    // 상품 생성
    public ItemCreateResponseDto createItem(Long userId, ItemCreateRequestDto request) {

        // 1. Store 존재 여부 + 소유자 검증
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new BusinessException(StoreErrorCode.STORE_NOT_FOUND));

        if (!store.getOwnerId().equals(userId)) {
            throw new BusinessException(StoreErrorCode.STORE_OWNER_FORBIDDEN);
        }

        // 2. Category 존재 여부 검증
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        // 3. 요청 내 SKU 중복 검증 (같은 요청 안에서 SKU가 겹치는 경우)
        long distinctSkuCount = request.getVariants().stream()
                .map(VariantDto::getSku)
                .distinct()
                .count();
        if (distinctSkuCount != request.getVariants().size()) {
            throw new BusinessException(CatalogErrorCode.DUPLICATE_SKU);
        }

        // 4. DB SKU 중복 검증
        for (VariantDto variantDto : request.getVariants()) {
            if (itemVariantRepository.existsBySku(variantDto.getSku())) {
                throw new BusinessException(CatalogErrorCode.DUPLICATE_SKU);
            }
        }

        // 5. 요청 내 옵션 조합 중복 검증
        Set<String> requestCombinations = new HashSet<>();
        for (VariantDto variantDto : request.getVariants()) {
            String combinationKey = buildOptionCombinationKey(
                    variantDto.getOptions(), OptionDto::getType, OptionDto::getValue);
            if (!requestCombinations.add(combinationKey)) {
                throw new BusinessException(CatalogErrorCode.DUPLICATE_OPTION_COMBINATION);
            }
        }

        // 6. Item 생성
        Item item = new Item(store, category, request.getName(), request.getBasePrice());
        itemRepository.save(item);

        // 7. Option 생성 — 동일 Item 내에서 type+value가 같은 옵션은 한 번만 생성
        Map<String, Option> optionCache = new HashMap<>();

        // 8. Variant + ItemVariantOption + Inventory 생성
        List<VariantResult> variantResults = new ArrayList<>();

        for (VariantDto variantDto : request.getVariants()) {
            // ItemVariant 생성
            ItemVariant variant = new ItemVariant(item, variantDto.getSku(), variantDto.getPrice());
            itemVariantRepository.save(variant);

            // 옵션 연결
            if (variantDto.getOptions() != null) {
                for (OptionDto optionDto : variantDto.getOptions()) {
                    Option option = findOrCreateOption(
                            item, optionDto.getType(), optionDto.getValue(), optionCache);
                    ItemVariantOption variantOption = new ItemVariantOption(variant, option);
                    itemVariantOptionRepository.save(variantOption);
                }
            }

            // Inventory 초기 재고 생성 (InventoryService 내부에서 음수 검증 포함)
            inventoryService.createInventory(variant.getItemVariantId(), variantDto.getInitialQuantity());

            variantResults.add(new VariantResult(
                    variant.getItemVariantId(),
                    variant.getSku(),
                    variant.getPrice(),
                    variant.getStatus(),
                    variantDto.getInitialQuantity()
            ));
        }

        return new ItemCreateResponseDto(
                item.getItemId(),
                store.getStoreId(),
                category.getCategoryId(),
                item.getName(),
                item.getBasePrice(),
                item.getStatus(),
                variantResults
        );
    }

    // 기존 상품에 Variant 추가
    public AddVariantsResponseDto addVariants(Long userId, Long itemId, AddVariantsRequestDto request) {

        // 1. Item 존재 + 소유자 검증
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ItemErrorCode.ITEM_NOT_FOUND));

        if (!item.getStore().getOwnerId().equals(userId)) {
            throw new BusinessException(StoreErrorCode.STORE_OWNER_FORBIDDEN);
        }

        // 2. 요청 내 SKU 중복 검증
        long distinctSkuCount = request.getVariants().stream()
                .map(AddVariantsRequestDto.VariantDto::getSku)
                .distinct()
                .count();
        if (distinctSkuCount != request.getVariants().size()) {
            throw new BusinessException(CatalogErrorCode.DUPLICATE_SKU);
        }

        // 3. DB SKU 중복 검증
        for (AddVariantsRequestDto.VariantDto variantDto : request.getVariants()) {
            if (itemVariantRepository.existsBySku(variantDto.getSku())) {
                throw new BusinessException(CatalogErrorCode.DUPLICATE_SKU);
            }
        }

        // 4. 요청 내 옵션 조합 중복 검증
        Set<String> requestCombinations = new HashSet<>();
        for (AddVariantsRequestDto.VariantDto variantDto : request.getVariants()) {
            String combinationKey = buildOptionCombinationKey(
                    variantDto.getOptions(),
                    AddVariantsRequestDto.OptionDto::getType,
                    AddVariantsRequestDto.OptionDto::getValue);
            if (!requestCombinations.add(combinationKey)) {
                throw new BusinessException(CatalogErrorCode.DUPLICATE_OPTION_COMBINATION);
            }
        }

        // 5. DB 기존 옵션 조합과 중복 검증
        Set<String> existingCombinations = new HashSet<>();
        for (ItemVariant existingVariant : itemVariantRepository.findByItem_ItemId(itemId)) {
            String existingKey = buildOptionCombinationKey(
                    existingVariant.getItemVariantOptions(),
                    vo -> vo.getOption().getType(),
                    vo -> vo.getOption().getValue());
            existingCombinations.add(existingKey);
        }
        for (String requestKey : requestCombinations) {
            if (existingCombinations.contains(requestKey)) {
                throw new BusinessException(CatalogErrorCode.DUPLICATE_OPTION_COMBINATION);
            }
        }

        // 6. Variant + Option + ItemVariantOption + Inventory 생성
        Map<String, Option> optionCache = new HashMap<>();
        List<AddVariantsResponseDto.VariantResult> variantResults = new ArrayList<>();

        for (AddVariantsRequestDto.VariantDto variantDto : request.getVariants()) {
            ItemVariant variant = new ItemVariant(item, variantDto.getSku(), variantDto.getPrice());
            itemVariantRepository.save(variant);

            if (variantDto.getOptions() != null) {
                for (AddVariantsRequestDto.OptionDto optionDto : variantDto.getOptions()) {
                    Option option = findOrCreateOption(
                            item, optionDto.getType(), optionDto.getValue(), optionCache);
                    ItemVariantOption variantOption = new ItemVariantOption(variant, option);
                    itemVariantOptionRepository.save(variantOption);
                }
            }

            inventoryService.createInventory(variant.getItemVariantId(), variantDto.getInitialQuantity());

            variantResults.add(new AddVariantsResponseDto.VariantResult(
                    variant.getItemVariantId(),
                    variant.getSku(),
                    variant.getPrice(),
                    variant.getStatus(),
                    variantDto.getInitialQuantity()
            ));
        }

        return new AddVariantsResponseDto(itemId, variantResults);
    }

    private <T> String buildOptionCombinationKey(
            Collection<T> options,
            Function<T, OptionType> typeExtractor,
            Function<T, String> valueExtractor) {
        if (options == null || options.isEmpty()) {
            return "";
        }
        return options.stream()
                .map(o -> typeExtractor.apply(o).name() + ":" + valueExtractor.apply(o).trim())
                .sorted()
                .collect(Collectors.joining("|"));
    }

    private Option findOrCreateOption(
            Item item, OptionType type, String value, Map<String, Option> optionCache) {
        String trimmed = value.trim();
        String cacheKey = type.name() + ":" + trimmed;
        return optionCache.computeIfAbsent(cacheKey, k ->
                optionRepository.findByItem_ItemIdAndTypeAndValue(item.getItemId(), type, trimmed)
                        .orElseGet(() -> optionRepository.save(new Option(item, type, trimmed)))
        );
    }

    // 단건 조회
    @Transactional(readOnly = true)
    public ItemResponse getItemById(Long id) {
        Item item = itemRepository.findByItemIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ItemErrorCode.ITEM_NOT_FOUND));
        return itemMapper.toResponse(item);
    }

    // 카테고리 기준 조회
    @Transactional(readOnly = true)
    public List<ItemResponse> getItemsByCategory(Long categoryId) {

        // 1. category 존재 여부 확인
        if (!categoryRepository.existsById(categoryId)) {
            throw new BusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND);
        }

        // 2. 상품 조회
        return itemRepository.findByCategory_CategoryIdAndIsDeletedFalse(categoryId)
            .stream()
            .map(itemMapper::toResponse)
            .toList();
    }

    // 스토어 기준 조회
    @Transactional(readOnly = true)
    public List<ItemResponse> getItemsByStore(Long storeId) {

        // 1. store 존재 여부 확인
        if (!storeRepository.existsById(storeId)) {
            throw new BusinessException(StoreErrorCode.STORE_NOT_FOUND);
        }

        // 2. 상품 조회
        return itemRepository.findByStore_StoreIdAndIsDeletedFalse(storeId)
            .stream()
            .map(itemMapper::toResponse)
            .toList();
    }

    // 기본 조회(정렬 / 페이징용)
    @Transactional(readOnly = true)
    public Page<ItemResponse> getItems(Pageable pageable) {
        return itemRepository.findByIsDeletedFalse(pageable)
            .map(itemMapper::toResponse);
    }
}
