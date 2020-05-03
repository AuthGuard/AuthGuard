package com.authguard.rest.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = AppDTO.class)
@JsonDeserialize(as = AppDTO.class)
public interface App {
    String getId();
    String getExternalId();
    String getName();
    String getAccountId();
    List<PermissionDTO> getPermissions();
    List<String> getScopes();
    List<String> getRoles();
    boolean isActive();
    boolean isDeleted();
}
