package com.nexblocks.authguard.api.dto.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = SessionDTO.class)
@JsonSerialize(as = SessionDTO.class)
public interface Session {
    long getId();
    String getSessionToken();
    long getAccountId();
    Instant getExpiresAt();
    boolean isForTracking();
    boolean isActive();
}
