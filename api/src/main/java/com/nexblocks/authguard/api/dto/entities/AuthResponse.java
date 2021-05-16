package com.nexblocks.authguard.api.dto.entities;

import com.nexblocks.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = AuthResponseDTO.class)
@JsonDeserialize(as = AuthResponseDTO.class)
public interface AuthResponse {
    String getType();
    Object getToken();
    Object getRefreshToken();
}
