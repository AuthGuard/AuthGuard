package com.nexblocks.authguard.api.dto.requests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.entities.ActionTokenRequestType;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = ActionTokenRequestDTO.class)
@JsonDeserialize(as = ActionTokenRequestDTO.class)
public interface ActionTokenRequest {
    ActionTokenRequestType getType();
    OtpRequestDTO getOtp();
    AuthRequestDTO getBasic();
    String getAction();
}
