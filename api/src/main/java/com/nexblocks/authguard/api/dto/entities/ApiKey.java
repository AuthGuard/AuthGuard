package com.nexblocks.authguard.api.dto.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

import java.time.OffsetDateTime;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = ApiKeyDTO.class)
@JsonDeserialize(as = ApiKeyDTO.class)
public interface ApiKey {
    String getId();
    OffsetDateTime getCreatedAt();
    OffsetDateTime getLastModified();
    String getAppId();
    String getKey();
}
