package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemVariantOptionRepository extends JpaRepository<ItemVariantOption, Long> {

  List<ItemVariantOption> findByItemVariant_ItemVariantId(Long itemVariantId);

  List<ItemVariantOption> findByOption_OptionId(Long optionId);

  boolean existsByItemVariant_ItemVariantIdAndOption_OptionId(Long itemVariantId, Long optionId);
}
