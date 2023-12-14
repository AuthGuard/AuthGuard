package com.nexblocks.authguard.api.dto.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = ClientDTO.class)
@JsonDeserialize(as = ClientDTO.class)
public interface Client {
    long getId();
    Instant getCreatedAt();
    Instant getLastModified();
    String getExternalId();
    String getName();
    Long getAccountId();
    String getDomain();
    String getBaseUrl();
    String getClientType();
    boolean isActive();
    boolean isDeleted();
}
