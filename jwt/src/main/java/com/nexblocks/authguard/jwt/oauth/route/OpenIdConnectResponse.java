package com.nexblocks.authguard.jwt.oauth.route;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenIdConnectResponse {
    private final String accessToken;
    private final String idToken;
    private final String refreshToken;
    private final long expiresIn;
    private final String tokenType = "Bearer";

    public OpenIdConnectResponse(String accessToken, String idToken, String refreshToken, long expiresIn) {
        this.accessToken = accessToken;
        this.idToken = idToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    @JsonProperty("access_token")
    public String getAccessToken() {
        return accessToken;
    }

    @JsonProperty("id_token")
    public String getIdToken() {
        return idToken;
    }

    @JsonProperty("refresh_token")
    public String getRefreshToken() {
        return refreshToken;
    }

    @JsonProperty("expires_in")
    public long getExpiresIn() {
        return expiresIn;
    }

    @JsonProperty("token_type")
    public String getTokenType() {
        return tokenType;
    }
}
