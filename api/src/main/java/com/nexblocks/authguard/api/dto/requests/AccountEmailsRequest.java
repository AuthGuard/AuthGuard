package com.nexblocks.authguard.api.dto.requests;

import com.nexblocks.authguard.api.dto.entities.AccountEmailDTO;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = AccountEmailsRequestDTO.class)
@JsonSerialize(as = AccountEmailsRequestDTO.class)
public interface AccountEmailsRequest {
    @Value.Default
    default boolean isBackup() {
        return false;
    }

    AccountEmailDTO getEmail();
}
