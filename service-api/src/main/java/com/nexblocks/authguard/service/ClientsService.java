package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.ClientBO;

import java.util.List;
import java.util.Optional;

public interface ClientsService extends IdempotentCrudService<ClientBO> {
    Optional<ClientBO> getByExternalId(String externalId);
    Optional<ClientBO> activate(String id);
    Optional<ClientBO> deactivate(String id);
    List<ClientBO> getByAccountId(String accountId);
}
