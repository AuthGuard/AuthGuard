package com.nexblocks.authguard.api.dto.requests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.entities.TokenRestrictionsDTO;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = AuthRequestDTO.class)
@JsonDeserialize(as = AuthRequestDTO.class)
public interface AuthRequest {
    String getIdentifier();
    String getPassword();
    String getToken();
    String getDomain();
    TokenRestrictionsDTO getRestrictions();
    String getDeviceId();
    String getExternalSessionId();
    String getUserAgent();
    String getSourceIp();
    String getClientId();
}
