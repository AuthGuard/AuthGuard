package com.nexblocks.authguard.api.dto.entities;

import com.nexblocks.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = AccountEmailDTO.class)
@JsonSerialize(as = AccountEmailDTO.class)
public interface AccountEmail {
    String getEmail();
    boolean isVerified();
}
