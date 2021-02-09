package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AppBO;

import java.util.List;
import java.util.Optional;

public interface ApplicationsService extends IdempotentCrudService<AppBO> {
    Optional<AppBO> getByExternalId(String externalId);
    Optional<AppBO> activate(String id);
    Optional<AppBO> deactivate(String id);
    List<AppBO> getByAccountId(String accountId);
}
