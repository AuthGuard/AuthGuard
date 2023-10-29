package com.nexblocks.authguard.jwt.oauth.route;

public class OpenIdConnectError {
    private final String error;
    private final String errorDescription;

    public OpenIdConnectError(String error, String errorDescription) {
        this.error = error;
        this.errorDescription = errorDescription;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }
}
