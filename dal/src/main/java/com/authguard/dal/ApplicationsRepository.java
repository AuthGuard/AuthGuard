package com.authguard.dal;

import com.authguard.dal.model.AppDO;

import java.util.List;
import java.util.Optional;

public interface ApplicationsRepository {
    AppDO save(AppDO app);
    Optional<AppDO> getById(String appId);
    Optional<AppDO> update(AppDO app);
    Optional<AppDO> delete(String appId);
    List<AppDO> getAllForAccount(String accountId);
}
