package com.nexblocks.authguard.api.dto.entities;

import com.nexblocks.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = UserIdentifierDTO.class)
@JsonDeserialize(as = UserIdentifierDTO.class)
public interface UserIdentifier extends DomainScoped {
    Type getType();
    String getIdentifier();

    @Value.Default
    default boolean isActive() {
        return true;
    }

    enum Type {
        USERNAME,
        EMAIL,
        PHONE_NUMBER
    }
}
