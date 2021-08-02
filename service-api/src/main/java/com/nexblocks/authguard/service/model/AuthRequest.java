package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface AuthRequest {
    String getIdentifier();
    String getPassword();
    String getToken();
    TokenRestrictionsBO getRestrictions();
    String getDeviceId();
    String getExternalSessionId();
    String getUserAgent();
    String getSourceIp();
}
