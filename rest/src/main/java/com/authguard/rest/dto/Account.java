package com.authguard.rest.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = AccountDTO.class)
@JsonDeserialize(as = AccountDTO.class)
public interface Account {
    String getId();
    String getExternalId();
    List<PermissionDTO> getPermissions();
    List<String> getScopes();
    List<String> getRoles();
    List<AccountEmailDTO> getEmails();
    boolean isActive();
    boolean isDeleted();
}
