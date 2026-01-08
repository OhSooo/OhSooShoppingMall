package com.ohsooo.platform.ohsooshoppingmall.store.dto;

import com.ohsooo.platform.ohsooshoppingmall.store.domain.StoreStatus;
import lombok.*;

@Getter
@NoArgsConstructor
public class StoreStatusChangeRequest {
    private StoreStatus status;
}
