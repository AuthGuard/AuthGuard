package com.nexblocks.authguard.api.dto.requests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.entities.DomainScoped;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;


@Value.Immutable
@DTOStyle
@JsonDeserialize(as = CreateClientRequestDTO.class)
@JsonSerialize(as = CreateClientRequestDTO.class)
public interface CreateClientRequest extends DomainScoped {
    String getExternalId();
    String getName();
    Long getAccountId();
    String getBaseUrl();
    ClientType getClientType();

    @Value.Default
    default boolean isActive() {
        return true;
    }

    enum ClientType {
        AUTH,
        ADMIN
    }
}
