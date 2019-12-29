package org.auther.service;

import org.auther.service.model.AppBO;

import java.util.Optional;

public interface ApplicationsService {
    AppBO create(AppBO app);
    Optional<AppBO> getById(String id);
    Optional<AppBO> update(AppBO app);
    Optional<AppBO> delete(String id);
}
