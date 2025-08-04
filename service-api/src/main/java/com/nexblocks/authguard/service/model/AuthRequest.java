package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface AuthRequest {
    String getIdentifier();
    @Value.Redacted
    String getPassword();
    @Value.Redacted
    String getToken();
    String getDomain();
    TokenRestrictionsBO getRestrictions();
    String getDeviceId();
    String getExternalSessionId();
    String getUserAgent();
    String getSourceIp();
    String getClientId();
    String getTrackingSession(); // Should only be used for SSO cases for now
    Object getExtraParameters();
}
