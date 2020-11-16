package com.authguard.api.dto.requests;

import com.authguard.api.dto.entities.AccountEmailDTO;
import com.authguard.api.dto.entities.PermissionDTO;
import com.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = CreateAccountRequestDTO.class)
@JsonSerialize(as = CreateAccountRequestDTO.class)
public interface CreateAccountRequest {
    String getExternalId();
    AccountEmailDTO getEmail();
    AccountEmailDTO getBackupEmail();
    List<PermissionDTO> getPermissions();
    List<String> getScopes();
    List<String> getRoles();
    boolean isActive();
}
