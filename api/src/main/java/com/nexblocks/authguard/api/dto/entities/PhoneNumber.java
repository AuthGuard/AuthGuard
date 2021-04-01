package com.nexblocks.authguard.api.dto.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = PhoneNumberDTO.class)
@JsonSerialize(as = PhoneNumberDTO.class)
public interface PhoneNumber {
    String getNumber();
    boolean isVerified();
    boolean isActive();
}
