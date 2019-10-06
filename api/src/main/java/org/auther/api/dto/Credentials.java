package org.auther.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = CredentialsDTO.class)
public interface Credentials {
    @Nullable String getId();
    String getAccountId();
    String getUsername();
    @Nullable String getPlainPassword();
}
