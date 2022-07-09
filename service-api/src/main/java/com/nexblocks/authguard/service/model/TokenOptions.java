package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface TokenOptions {
    String getSource();
    String getDeviceId();
    String getClientId();
    String getExternalSessionId();
    String getUserAgent();
    String getSourceIp();
}
