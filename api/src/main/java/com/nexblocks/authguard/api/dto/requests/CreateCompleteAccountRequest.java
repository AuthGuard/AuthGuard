package com.nexblocks.authguard.api.dto.requests;

import com.nexblocks.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = CreateCompleteAccountRequestDTO.class)
@JsonSerialize(as = CreateCompleteAccountRequestDTO.class)
@Deprecated
public interface CreateCompleteAccountRequest {
    CreateAccountRequestDTO getAccount();
    CreateCredentialsRequestDTO getCredentials();
}
