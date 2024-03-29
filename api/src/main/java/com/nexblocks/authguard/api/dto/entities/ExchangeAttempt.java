package com.nexblocks.authguard.api.dto.entities;

import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
@DTOStyle
public interface ExchangeAttempt {
    String getId();
    Instant getCreatedAt();
    Instant getLastModified();
    String getEntityId();
    String getExchangeFrom();
    String getExchangeTo();
    boolean isSuccessful();
    String getDeviceId();
    String getClientId();
    String getExternalSessionId();
    String getSourceIp();
    String getUserAgent();
}
