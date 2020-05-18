package com.authguard.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = OtpRequestDTO.class)
@JsonSerialize(as = OtpRequestDTO.class)
public interface OtpRequest {
    String getPasswordId();
    String getPassword();
}
