package com.nexblocks.authguard.jwt.oauth.route;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
public interface OpenIdConnectRequest {
    String getResponseType();
    String getClientId();
    List<String> getScope();
    String getState();
    String getRedirectUri();
}
