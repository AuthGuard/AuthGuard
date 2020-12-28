package com.authguard.api.dto.requests;

import com.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = CreateCompleteAccountResponseDTO.class)
@JsonSerialize(as = CreateCompleteAccountResponseDTO.class)
public interface CreateCompleteAccountResponse {
    String getAccountId();
    String getCredentialsId();
}
