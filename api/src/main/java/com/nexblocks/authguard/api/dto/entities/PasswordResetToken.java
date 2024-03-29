package com.nexblocks.authguard.api.dto.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = PasswordResetTokenDTO.class)
@JsonDeserialize(as = PasswordResetTokenDTO.class)
public interface PasswordResetToken {
    String getToken();
    long getIssuedAt();
    long getExpiresAt();
}
