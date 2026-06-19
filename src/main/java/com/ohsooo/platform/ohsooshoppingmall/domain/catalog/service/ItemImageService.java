package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request.ItemImageCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request.ItemImageOrderUpdateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request.ItemImageUpdateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemImageResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Item;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemImage;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.exception.ItemErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.exception.ItemImageErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.mapper.ItemImageMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemImageRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.exception.StoreErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ItemImageService {

    private static final int MAX_IMAGE_COUNT = 10;

    private final ItemImageRepository itemImageRepository;
    private final ItemRepository itemRepository;
    private final ItemImageMapper itemImageMapper;
    private final EntityManager entityManager;

    public ItemImageResponse addImage(Long userId, Long itemId, ItemImageCreateRequestDto request) {
        Item item = getActiveItemOrThrow(itemId);
        validateStoreOwner(item, userId);

        int currentCount = itemImageRepository.countByItem_ItemId(itemId);
        if (currentCount >= MAX_IMAGE_COUNT) {
            throw new BusinessException(ItemImageErrorCode.IMAGE_LIMIT_EXCEEDED);
        }

        int displayOrder = itemImageRepository.findMaxDisplayOrderByItemId(itemId) + 1;
        boolean isPrimary = request.getIsPrimary() != null && request.getIsPrimary();

        if (!isPrimary && (currentCount == 0 || !itemImageRepository.existsByItem_ItemIdAndIsPrimaryTrue(itemId))) {
            isPrimary = true;
        }

        if (isPrimary) {
            itemImageRepository.clearPrimaryByItemId(itemId);
            entityManager.flush();
        }

        ItemImage image = new ItemImage(item, request.getImageUrl(), displayOrder, isPrimary, request.getAltText());
        itemImageRepository.save(image);

        return itemImageMapper.toResponse(image);
    }

    @Transactional(readOnly = true)
    public List<ItemImageResponse> getImagesByItemId(Long itemId) {
        if (!itemRepository.findByItemIdAndIsDeletedFalse(itemId).isPresent()) {
            throw new BusinessException(ItemErrorCode.ITEM_NOT_FOUND);
        }

        return itemImageRepository.findByItem_ItemIdOrderByDisplayOrderAscItemImageIdAsc(itemId)
                .stream()
                .map(itemImageMapper::toResponse)
                .toList();
    }

    public ItemImageResponse updateImage(Long userId, Long itemId, Long imageId, ItemImageUpdateRequestDto request) {
        Item item = getActiveItemOrThrow(itemId);
        validateStoreOwner(item, userId);

        ItemImage image = itemImageRepository.findByItemImageIdAndItem_ItemId(imageId, itemId)
                .orElseThrow(() -> new BusinessException(ItemImageErrorCode.IMAGE_NOT_FOUND));

        if (request.getImageUrl() != null) {
            String trimmedUrl = request.getImageUrl().trim();
            if (trimmedUrl.isEmpty()) {
                throw new BusinessException(ItemImageErrorCode.INVALID_IMAGE_URL);
            }
            image.updateImageUrl(trimmedUrl);
        }

        if (request.getIsPrimary() != null) {
            if (!request.getIsPrimary() && image.isPrimary()) {
                throw new BusinessException(ItemImageErrorCode.PRIMARY_IMAGE_REQUIRED);
            }
            if (request.getIsPrimary() && !image.isPrimary()) {
                itemImageRepository.clearPrimaryByItemIdExcluding(itemId, imageId);
                entityManager.flush();
            }
            image.updateIsPrimary(request.getIsPrimary());
        }

        if (request.getAltText() != null) {
            image.updateAltText(request.getAltText());
        }

        return itemImageMapper.toResponse(image);
    }

    public void deleteImage(Long userId, Long itemId, Long imageId) {
        Item item = getActiveItemOrThrow(itemId);
        validateStoreOwner(item, userId);

        ItemImage image = itemImageRepository.findByItemImageIdAndItem_ItemId(imageId, itemId)
                .orElseThrow(() -> new BusinessException(ItemImageErrorCode.IMAGE_NOT_FOUND));

        boolean wasPrimary = image.isPrimary();
        itemImageRepository.delete(image);
        entityManager.flush();

        if (wasPrimary) {
            itemImageRepository.findFirstByItem_ItemIdOrderByDisplayOrderAscItemImageIdAsc(itemId)
                    .ifPresent(next -> next.updateIsPrimary(true));
        }
    }

    public List<ItemImageResponse> updateImageOrder(Long userId, Long itemId, ItemImageOrderUpdateRequestDto request) {
        Item item = getActiveItemOrThrow(itemId);
        validateStoreOwner(item, userId);

        List<Long> imageIds = request.getImageIds();

        if (imageIds.isEmpty()) {
            throw new BusinessException(ItemImageErrorCode.IMAGE_ORDER_EMPTY);
        }

        Set<Long> uniqueIds = new HashSet<>(imageIds);
        if (uniqueIds.size() != imageIds.size()) {
            throw new BusinessException(ItemImageErrorCode.IMAGE_ORDER_DUPLICATE);
        }

        int totalCount = itemImageRepository.countByItem_ItemId(itemId);
        if (imageIds.size() != totalCount) {
            throw new BusinessException(ItemImageErrorCode.IMAGE_ORDER_MISMATCH);
        }

        List<ItemImage> images = itemImageRepository.findByItem_ItemIdAndItemImageIdIn(itemId, imageIds);
        if (images.size() != imageIds.size()) {
            throw new BusinessException(ItemImageErrorCode.IMAGE_ORDER_MISMATCH);
        }

        Map<Long, ItemImage> imageMap = images.stream()
                .collect(Collectors.toMap(ItemImage::getItemImageId, Function.identity()));

        for (int i = 0; i < imageIds.size(); i++) {
            ItemImage image = imageMap.get(imageIds.get(i));
            image.updateDisplayOrder(i + 1);
        }

        return itemImageRepository.findByItem_ItemIdOrderByDisplayOrderAscItemImageIdAsc(itemId)
                .stream()
                .map(itemImageMapper::toResponse)
                .toList();
    }

    private Item getActiveItemOrThrow(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ItemErrorCode.ITEM_NOT_FOUND));

        if (item.isDeleted()) {
            throw new BusinessException(ItemImageErrorCode.ITEM_DELETED);
        }

        return item;
    }

    private void validateStoreOwner(Item item, Long userId) {
        if (!item.getStore().getOwnerId().equals(userId)) {
            throw new BusinessException(StoreErrorCode.STORE_OWNER_FORBIDDEN);
        }
    }

}
