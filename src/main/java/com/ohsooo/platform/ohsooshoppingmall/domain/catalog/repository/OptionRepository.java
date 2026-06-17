package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.Option;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.option.OptionType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionRepository extends JpaRepository<Option, Long> {

  // 아이템 ID 기준 옵션 조회
  List<Option> findByItem_ItemId(Long itemId);

  // 아이템 + 옵션 타입 기준 조회
  List<Option> findByItem_ItemIdAndType(Long itemId, OptionType type);

  // 특정 옵션 존재 여부 체크 (중복 방지용)
  boolean existsByItem_ItemIdAndTypeAndValue(Long itemId, OptionType type, String value);

  // 아이템 + 타입 + 값 기준 옵션 단건 조회 (기존 옵션 재사용용)
  Optional<Option> findByItem_ItemIdAndTypeAndValue(Long itemId, OptionType type, String value);
}
