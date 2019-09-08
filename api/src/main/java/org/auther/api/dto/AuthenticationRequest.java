package org.auther.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = AuthenticationRequestDTO.class)
@JsonDeserialize(as = AuthenticationRequestDTO.class)
public interface AuthenticationRequest {
    String getAuthorization();
}
