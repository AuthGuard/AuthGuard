package com.nexblocks.authguard.api.dto.requests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = UpdateRoleRequestDTO.class)
@JsonSerialize(as = UpdateRoleRequestDTO.class)
public interface UpdateRoleRequest {
    boolean isForAccounts();
    boolean isForApplications();
}
