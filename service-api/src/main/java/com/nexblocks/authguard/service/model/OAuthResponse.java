package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface OAuthResponse {
    String getAccessToken();
    String getIdToken();
    String getRefreshToken();
}
