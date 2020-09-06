package com.authguard.api.dto.entities;

import com.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = TokensDTO.class)
@JsonDeserialize(as = TokensDTO.class)
public interface Tokens {
    String getType();
    Object getToken();
    Object getRefreshToken();
}
