package com.authguard.api.dto.requests;

import com.authguard.api.dto.entities.AccountEmailDTO;
import com.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = AccountEmailsRequestDTO.class)
@JsonSerialize(as = AccountEmailsRequestDTO.class)
public interface AccountEmailsRequest {
    List<AccountEmailDTO> getEmails();
}
