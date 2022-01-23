package com.nexblocks.authguard.api.dto.requests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = PasswordResetRequestDTO.class)
@JsonDeserialize(as = PasswordResetRequestDTO.class)
public interface PasswordResetRequest {
    boolean isByToken();
    String getResetToken();
    String getIdentifier();
    String getOldPassword();
    String getNewPassword();
    String getDomain();
}
