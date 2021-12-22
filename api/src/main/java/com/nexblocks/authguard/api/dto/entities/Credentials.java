package com.nexblocks.authguard.api.dto.entities;

import com.nexblocks.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.time.OffsetDateTime;
import java.util.List;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = CredentialsDTO.class)
public interface Credentials {
    String getId();
    OffsetDateTime getCreatedAt();
    OffsetDateTime getLastModified();
    OffsetDateTime getPasswordUpdatedAt();
    String getAccountId();
    List<UserIdentifierDTO> getIdentifiers();
    String getPlainPassword();
}
