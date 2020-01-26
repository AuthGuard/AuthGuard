package com.authguard.service;

import com.authguard.service.model.AppBO;

import java.util.List;
import java.util.Optional;

public interface ApplicationsService {
    AppBO create(AppBO app);
    Optional<AppBO> getById(String id);
    Optional<AppBO> update(AppBO app);
    Optional<AppBO> delete(String id);
    List<AppBO> getByAccountId(String accountId);
}
