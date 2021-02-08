package com.authguard.api.dto.entities;

import com.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.OffsetDateTime;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = PermissionDTO.class)
@JsonDeserialize(as = PermissionDTO.class)
public interface Permission {
    String getId();
    OffsetDateTime getCreatedAt();
    OffsetDateTime getLastModified();
    String getGroup();
    String getName();
}
