package com.nexblocks.authguard.api.dto.requests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = ApiKeyVerificationRequestDTO.class)
@JsonDeserialize(as = ApiKeyVerificationRequestDTO.class)
public interface ApiKeyVerificationRequest {
    String getKey();
    String getKeyType();
}
