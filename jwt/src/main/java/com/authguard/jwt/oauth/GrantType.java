package com.authguard.jwt.oauth;

public enum  GrantType {
    AUTHORIZATION_CODE("authorization_code");

    private final String type;

    GrantType(final String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }
}
