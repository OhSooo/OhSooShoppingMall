package com.ohsooo.platform.ohsooshoppingmall.store.dto;

import lombok.Getter;

@Getter
public class CreateStoreRequest {

    private Long ownerId;
    private String name;
    private String description;

    protected CreateStoreRequest() {
    }
}