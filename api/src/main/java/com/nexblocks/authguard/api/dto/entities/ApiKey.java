package com.nexblocks.authguard.api.dto.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = ApiKeyDTO.class)
@JsonDeserialize(as = ApiKeyDTO.class)
public interface ApiKey {
    long getId();
    Instant getCreatedAt();
    Instant getLastModified();
    Long getAppId();
    String getKey();
    String getType();
    boolean isForClient();
    Instant getExpiresAt();
}
