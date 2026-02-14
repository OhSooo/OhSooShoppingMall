package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemVariantOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemVariantOptionRepository extends JpaRepository<ItemVariantOption, Long> {

  // 특정 variant에 포함된 옵션들 조회
  List<ItemVariantOption> findByItemVariant_ItemVariantId(Long itemVariantId);

  // 특정 option이 사용된 variant들 조회 (관리/분석용)
  List<ItemVariantOption> findByOption_OptionId(Long optionId);

  // 특정 variant + 특정 option 존재 여부 (중복 방지용)
  boolean existsByItemVariant_ItemVariantIdAndOption_OptionId(Long itemVariantId, Long optionId);
}
