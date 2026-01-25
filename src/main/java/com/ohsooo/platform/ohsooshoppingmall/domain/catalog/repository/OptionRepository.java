package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.Option;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.OptionType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionRepository extends JpaRepository<Option, Long> {

  List<Option> findByItem_ItemId(Long itemId);

  List<Option> findByItem_ItemIdAndType(Long itemId, OptionType type);
}
