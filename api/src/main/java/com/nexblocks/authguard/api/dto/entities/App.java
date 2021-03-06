package com.nexblocks.authguard.api.dto.entities;

import com.nexblocks.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.OffsetDateTime;
import java.util.List;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = AppDTO.class)
@JsonDeserialize(as = AppDTO.class)
public interface App {
    String getId();
    OffsetDateTime getCreatedAt();
    OffsetDateTime getLastModified();
    String getExternalId();
    String getName();
    String getAccountId();
    List<PermissionDTO> getPermissions();
    List<String> getRoles();
    boolean isActive();
    boolean isDeleted();
}
