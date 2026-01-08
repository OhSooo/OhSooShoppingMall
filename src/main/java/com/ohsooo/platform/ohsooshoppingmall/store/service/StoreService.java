package com.ohsooo.platform.ohsooshoppingmall.store.service;
import com.ohsooo.platform.ohsooshoppingmall.store.domain.StoreStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ohsooo.platform.ohsooshoppingmall.store.domain.Store;
import com.ohsooo.platform.ohsooshoppingmall.store.repository.StoreRepository;

import java.util.List;

@Service
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;


    // 생성자 주입 (권장 방식)
    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    /**
     * 스토어 생성
     * - 트랜잭션 필요 (쓰기)
     * - status, createdAt, updatedAt은 엔티티에서 자동 설정
     */
    public Store createStore(Long ownerId, String name, String description) {
        Store store = new Store(ownerId, name, description);
        return storeRepository.save(store);
    }

    /**
     * 스토어 단건 조회
     * - 조회 전용 트랜잭션
     * - 존재하지 않으면 예외
     */
    @Transactional(readOnly = true)
    public Store getStore(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("스토어가 존재하지 않습니다"));
    }


    /**
     * 활성화된 스토어 목록 조회
     * - 사용자에게 노출되는 공개 목록 용도
     * - status = ACTIVE 조건
     */
    @Transactional(readOnly = true)
    public List<Store> getActiveStores() {
        return storeRepository.findAllByStatus(StoreStatus.ACTIVE);
    }

    /**
     * 스토어 상태 변경 (Owner용)
     * - 본인 소유 스토어만 변경 가능
     * - Owner는 SUSPENDED 상태로 변경할 수 없음 (관리자 전용)
     * - 실제 DB 삭제 없이 status 값만 변경하는 Soft Delete 구조
     */
    public void changeStatusByOwner(Long storeId, Long ownerId, StoreStatus newStatus) {
        Store store = getStore(storeId);

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
        Store store = getStore(storeId);
        store.changeStatus(newStatus);
    }
}