package com.flow.demo.gateway.enums;

public enum RSocketCommunicationType {
    CHANNEL("/channel"), STREAM("/stream"), REQUEST_RESPONSE("/request-response"), REQUEST_FORGET("/request-forget");

    private final String type;

    RSocketCommunicationType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
