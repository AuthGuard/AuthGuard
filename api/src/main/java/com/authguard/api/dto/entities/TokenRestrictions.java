package com.authguard.api.dto.entities;

import com.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = TokenRestrictionsDTO.class)
@JsonSerialize(as = TokenRestrictionsDTO.class)
public interface TokenRestrictions {
    List<String> getPermissions();
    List<String> getScopes();
}
