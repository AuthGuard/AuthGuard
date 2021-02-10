package com.nexblocks.authguard.rest.mappers;

import com.nexblocks.authguard.api.dto.entities.AccountDTO;
import com.nexblocks.authguard.api.dto.requests.CreateAccountRequestDTO;

public class AccountsMapper {
    public AccountDTO toAccountDTO(final CreateAccountRequestDTO request) {
        return AccountDTO.builder()
                .externalId(request.getExternalId())
                .email(request.getEmail())
                .backupEmail(request.getBackupEmail())
                .build();
    }
}
