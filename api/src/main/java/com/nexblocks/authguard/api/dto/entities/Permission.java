package com.nexblocks.authguard.api.dto.entities;

import com.nexblocks.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = PermissionDTO.class)
@JsonDeserialize(as = PermissionDTO.class)
public interface Permission extends DomainScoped {
    String getId();
    Instant getCreatedAt();
    Instant getLastModified();
    String getGroup();
    String getName();
    boolean isForAccounts();
    boolean isForApplications();
}
