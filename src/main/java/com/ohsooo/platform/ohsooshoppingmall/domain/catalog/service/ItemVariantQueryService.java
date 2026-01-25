package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.exception.CatalogErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemVariantRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemVariantQueryService {

  private final ItemVariantRepository itemVariantRepository;

  /**
   * 상품 판매 단위(ItemVariant)를 ID로 조회한다.
   *
   * - 사용처: 장바구니/주문/결제 과정에서 variantId로 상세를 확인할 때
   * - 조회 전용 서비스이므로 readOnly 트랜잭션을 사용한다.
   * - 존재하지 않는 경우 CatalogErrorCode에 해당하는 예외를 발생시킨다.
   *
   * @param itemVariantId 조회할 ItemVariant의 ID
   * @return 조회된 ItemVariant 엔티티
   * @throws IllegalArgumentException 존재하지 않는 ItemVariant ID인 경우
   */
  public ItemVariant getById(Long itemVariantId) {
    return itemVariantRepository.findById(itemVariantId)
        .orElseThrow(() ->
            new IllegalArgumentException(CatalogErrorCode.ITEM_VARIANT_NOT_FOUND.getMessage())
        );
  }

  /**
   * 특정 상품(Item)에 속한 모든 판매 단위(ItemVariant)를 조회한다.
   *
   * - 상품 상세 페이지에서 "옵션 선택(사이즈/색상)"을 구성할 때 사용된다.
   * - 예: 같은 Item이라도 옵션 조합에 따라 SKU/가격/재고가 다른 여러 Variant가 존재할 수 있다.
   * - 이 메서드는 해당 Item에 매핑된 Variant 목록을 반환한다.
   *
   * @param itemId 상품 ID
   * @return 해당 상품에 속한 ItemVariant 목록
   */
  public List<ItemVariant> getByItemId(Long itemId) {
    return itemVariantRepository.findByItem_ItemId(itemId);
  }

  /**
   * SKU로 판매 단위(ItemVariant)를 조회한다.
   *
   * - SKU는 판매 단위를 식별하는 유니크 키로 사용된다.
   * - 사용처: 내부 운영/관리 기능(재고 확인, 정산, CS 대응) 또는 외부 시스템 연동 시 활용 가능
   * - 존재하지 않는 SKU인 경우 CatalogErrorCode에 해당하는 예외를 발생시킨다.
   *
   * @param sku 판매 단위 SKU (UNIQUE)
   * @return 조회된 ItemVariant 엔티티
   * @throws IllegalArgumentException 존재하지 않는 SKU인 경우
   */
  public ItemVariant getBySku(String sku) {
    return itemVariantRepository.findBySku(sku)
        .orElseThrow(() ->
            new IllegalArgumentException(CatalogErrorCode.ITEM_VARIANT_NOT_FOUND.getMessage())
        );
  }
}
