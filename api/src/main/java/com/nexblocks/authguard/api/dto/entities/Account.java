package com.nexblocks.authguard.api.dto.entities;

import com.nexblocks.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.Instant;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = AccountDTO.class)
@JsonDeserialize(as = AccountDTO.class)
public interface Account extends DomainScoped {
    String getId();
    Instant getCreatedAt();
    Instant getLastModified();

    String getExternalId();

    boolean isSocial();
    String getIdentityProvider();

    String getFirstName();
    String getMiddleName();
    String getLastName();
    String getFullName();

    List<PermissionDTO> getPermissions();
    List<String> getRoles();
    AccountEmailDTO getEmail();
    AccountEmailDTO getBackupEmail();
    PhoneNumberDTO getPhoneNumber();

    Instant getPasswordUpdatedAt();
    List<UserIdentifierDTO> getIdentifiers();
    Integer getPasswordVersion();

    Map<String, String> getMetadata();

    boolean isActive();
    boolean isDeleted();
}
