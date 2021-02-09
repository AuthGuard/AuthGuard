package com.nexblocks.authguard.api.dto.entities;

import com.nexblocks.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = UserIdentifierDTO.class)
@JsonDeserialize(as = UserIdentifierDTO.class)
public interface UserIdentifier {
    Type getType();
    String getIdentifier();

    enum Type {
        USERNAME,
        EMAIL,
        PHONE_NUMBER
    }
}
