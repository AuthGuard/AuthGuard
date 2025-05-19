package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.AppDO;
import com.nexblocks.authguard.dal.repository.Repository;

import java.util.List;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

public interface ApplicationsRepository extends Repository<AppDO> {
    Uni<Optional<AppDO>> getByExternalId(String externalId);
    Uni<List<AppDO>> getAllForAccount(long accountId, Page<Long> page);
}
