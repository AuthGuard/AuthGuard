package com.authguard.api.dto.requests;

import com.authguard.api.dto.entities.UserIdentifierDTO;
import com.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = CreateCredentialsRequestDTO.class)
@JsonSerialize(as = CreateCredentialsRequestDTO.class)
public interface CreateCredentialsRequest {
    String getAccountId();
    List<UserIdentifierDTO> getIdentifiers();
    String getPlainPassword();
}
