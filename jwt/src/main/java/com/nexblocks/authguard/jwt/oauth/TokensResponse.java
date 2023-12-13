package com.nexblocks.authguard.jwt.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class TokensResponse {
    @JsonProperty("id_token")
    private String idToken;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    private long accountId;

    public String getIdToken() {
        return idToken;
    }

    public TokensResponse setIdToken(final String idToken) {
        this.idToken = idToken;
        return this;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public TokensResponse setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public TokensResponse setRefreshToken(final String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }

    public long getAccountId() {
        return accountId;
    }

    public TokensResponse setAccountId(final long accountId) {
        this.accountId = accountId;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final TokensResponse that = (TokensResponse) o;
        return Objects.equals(idToken, that.idToken) &&
                Objects.equals(accessToken, that.accessToken) &&
                Objects.equals(refreshToken, that.refreshToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idToken, accessToken, refreshToken);
    }
}
