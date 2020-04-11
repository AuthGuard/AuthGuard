package com.authguard.rest.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = CredentialsDTO.class)
public interface Credentials {
    String getId();
    String getAccountId();
    List<UserIdentifierDTO> getIdentifiers();
    String getPlainPassword();
}
