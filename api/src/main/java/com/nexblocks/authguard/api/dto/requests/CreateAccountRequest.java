package com.nexblocks.authguard.api.dto.requests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.entities.*;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = CreateAccountRequestDTO.class)
@JsonSerialize(as = CreateAccountRequestDTO.class)
public interface CreateAccountRequest extends DomainScoped {
    String getExternalId();

    String getFirstName();
    String getMiddleName();
    String getLastName();
    String getFullName();

    AccountEmailDTO getEmail();
    AccountEmailDTO getBackupEmail();
    PhoneNumberDTO getPhoneNumber();

    List<PermissionDTO> getPermissions();
    List<String> getRoles();

    List<UserIdentifierDTO> getIdentifiers();
    String getPlainPassword();

    Map<String, String> getMetadata();

    @Value.Default
    default boolean isActive() {
        return true;
    }
}
