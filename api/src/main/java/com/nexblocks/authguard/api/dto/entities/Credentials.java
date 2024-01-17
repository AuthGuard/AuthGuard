package com.nexblocks.authguard.api.dto.entities;

import com.nexblocks.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.List;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = CredentialsDTO.class)
public interface Credentials {
    String getId();
    Instant getCreatedAt();
    Instant getLastModified();
    Instant getPasswordUpdatedAt();
    String getAccountId();
    List<UserIdentifierDTO> getIdentifiers();
    String getPlainPassword();
    Integer getPasswordVersion();
    String getDomain();
}
