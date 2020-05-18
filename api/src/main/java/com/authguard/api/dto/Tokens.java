package com.authguard.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = TokensDTO.class)
@JsonDeserialize(as = TokensDTO.class)
public interface Tokens {
    String getType();
    String getToken();
    String getRefreshToken();
}
