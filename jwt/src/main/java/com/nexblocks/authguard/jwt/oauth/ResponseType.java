package com.nexblocks.authguard.jwt.oauth;

public enum ResponseType {
    TOKEN("token"),
    CODE("code");

    private final String type;

    ResponseType(final String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }
}
