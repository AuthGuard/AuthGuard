package com.nexblocks.authguard.api.dto.requests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = ApiKeyRequestDTO.class)
@JsonDeserialize(as = ApiKeyRequestDTO.class)
public interface ApiKeyRequest {
    boolean isForClient();
    String getKeyType();
    String getAppId();
    String getName();
    Instant getExpiresAt();
    DurationRequestDTO getValidFor();
}
