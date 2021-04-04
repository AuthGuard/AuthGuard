package com.nexblocks.authguard.api.dto.requests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.entities.AccountEmailDTO;
import com.nexblocks.authguard.api.dto.entities.PhoneNumberDTO;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = UpdateAccountRequestDTO.class)
@JsonSerialize(as = UpdateAccountRequestDTO.class)
public interface UpdateAccountRequest {
    String getFirstName();
    String getMiddleName();
    String getLastName();
    String getFullName();

    AccountEmailDTO getEmail();
    AccountEmailDTO getBackupEmail();
    PhoneNumberDTO getPhoneNumber();

}
