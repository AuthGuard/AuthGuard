package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AppBO;

import java.util.List;
import java.util.Optional;

public interface ApplicationsService extends IdempotentCrudService<AppBO> {
    Optional<AppBO> getByExternalId(long externalId);
    Optional<AppBO> activate(long id);
    Optional<AppBO> deactivate(long id);
    List<AppBO> getByAccountId(long accountId);
}
