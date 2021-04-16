package com.nexblocks.authguard.api.dto.entities;

import com.nexblocks.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.OffsetDateTime;
import java.util.List;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = AccountDTO.class)
@JsonDeserialize(as = AccountDTO.class)
public interface Account {
    String getId();
    OffsetDateTime getCreatedAt();
    OffsetDateTime getLastModified();

    String getExternalId();

    String getFirstName();
    String getMiddleName();
    String getLastName();
    String getFullName();

    List<PermissionDTO> getPermissions();
    List<String> getRoles();
    AccountEmailDTO getEmail();
    AccountEmailDTO getBackupEmail();
    PhoneNumberDTO getPhoneNumber();

    boolean isActive();
    boolean isDeleted();
}
