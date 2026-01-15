package com.ohsooo.platform.ohsooshoppingmall.domain.store.service;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response.StoreResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.StoreStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.Store;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.repository.StoreRepository;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreMapper storeMapper;

    /**
     * 스토어 생성
     * - 트랜잭션 필요 (쓰기)
     * - status, createdAt, updatedAt은 엔티티에서 자동 설정
     */
    public StoreResponse createStore(Long ownerId, String name, String description) {
        Store store = new Store(ownerId, name, description);
        Store saved = storeRepository.save(store);
        return storeMapper.toStoreResponse(saved);
    }

    /**
     * 스토어 단건 조회
     * - 조회 전용 트랜잭션
     * - 존재하지 않으면 예외
     */
    @Transactional(readOnly = true)
    public StoreResponse getStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("스토어가 존재하지 않습니다"));
        return storeMapper.toStoreResponse(store);
    }

    /**
     * 스토어 엔티티 단건 조회 (Service 내부 전용)
     * - 조회 전용 트랜잭션
     * - Controller에서는 사용하지 않음
     * - 비즈니스 로직(상태 변경, 소유자 검증 등)에서 사용
     * - 존재하지 않으면 예외
     */
    @Transactional(readOnly = true)
    protected Store getStoreEntity(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("스토어가 존재하지 않습니다"));
    }

    /**
     * 활성화된 스토어 목록 조회
     * - 사용자에게 노출되는 공개 목록 용도
     * - status = ACTIVE 조건
     */
    @Transactional(readOnly = true)
    public List<StoreResponse> getActiveStores() {
        List<Store> storeList = storeRepository.findAllByStatus(StoreStatus.ACTIVE);
        return storeList.stream().map(storeMapper::toStoreResponse).toList();
    }

    /**
     * 스토어 상태 변경 (Owner용)
     * - 본인 소유 스토어만 변경 가능
     * - Owner는 SUSPENDED 상태로 변경할 수 없음 (관리자 전용)
     * - 실제 DB 삭제 없이 status 값만 변경하는 Soft Delete 구조
     */
    public void changeStatusByOwner(Long storeId, Long ownerId, StoreStatus newStatus) {
        Store store = getStoreEntity(storeId);

        // 본인 스토어 검증
        if (!store.getOwnerId().equals(ownerId)) {
            throw new IllegalStateException("본인 스토어만 변경할 수 있습니다");
        }

        // 허용 상태 제한
        if (newStatus == StoreStatus.SUSPENDED) {
            throw new IllegalArgumentException("해당 상태로 변경할 수 없습니다");
        }

        store.changeStatus(newStatus);
    }

    /**
     * 스토어 상태 변경 (Admin용)
     * - 관리자 권한 전용
     * - 모든 스토어 대상
     * - 모든 상태 변경 가능 (ACTIVE / INACTIVE / SUSPENDED / DELETED)
     */
    public void changeStatusByAdmin(Long storeId, StoreStatus newStatus) {
        Store store = getStoreEntity(storeId);
        store.changeStatus(newStatus);
    }
}