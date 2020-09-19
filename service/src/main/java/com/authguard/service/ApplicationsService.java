package com.authguard.service;

import com.authguard.service.model.AppBO;
import com.authguard.service.model.RequestContextBO;

import java.util.List;
import java.util.Optional;

public interface ApplicationsService {
    AppBO create(AppBO app, RequestContextBO requestContext);
    Optional<AppBO> getById(String id);
    Optional<AppBO> getByExternalId(String externalId);
    Optional<AppBO> update(AppBO app);
    Optional<AppBO> delete(String id);
    Optional<AppBO> activate(String id);
    Optional<AppBO> deactivate(String id);
    List<AppBO> getByAccountId(String accountId);
}
