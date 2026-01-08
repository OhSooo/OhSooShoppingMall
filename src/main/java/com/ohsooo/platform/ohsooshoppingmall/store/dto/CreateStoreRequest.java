package com.ohsooo.platform.ohsooshoppingmall.store.dto;

public class CreateStoreRequest {

    private Long ownerId;
    private String name;
    private String description;

    protected CreateStoreRequest() {
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}