package com.authguard.api.dto.requests;

import com.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = RolesRequestDTO.class)
@JsonDeserialize(as = RolesRequestDTO.class)
public interface RolesRequest {
    Action getAction();
    List<String> getRoles();

    enum Action {
        GRANT,
        REVOKE
    }
}
