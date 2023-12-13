package com.nexblocks.authguard.api.dto.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = ActionTokenDTO.class)
@JsonDeserialize(as = ActionTokenDTO.class)
public interface ActionToken {
    String getToken();
    String getAction();
    Long getAccountId();
    long getValidFor();
}
