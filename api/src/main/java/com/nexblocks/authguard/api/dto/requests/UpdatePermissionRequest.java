package com.nexblocks.authguard.api.dto.requests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = UpdatePermissionRequestDTO.class)
@JsonSerialize(as = UpdatePermissionRequestDTO.class)
public interface UpdatePermissionRequest {
    boolean isForAccounts();
    boolean isForApplications();
}

