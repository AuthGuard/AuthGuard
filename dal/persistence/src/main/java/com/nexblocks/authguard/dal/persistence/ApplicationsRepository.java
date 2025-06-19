package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.AppDO;
import com.nexblocks.authguard.dal.model.PermissionDO;
import com.nexblocks.authguard.dal.repository.Repository;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Optional;

public interface ApplicationsRepository extends Repository<AppDO> {
    Uni<Optional<AppDO>> getByExternalId(String externalId);
    Uni<List<AppDO>> getAllForAccount(long accountId, Page<Long> page);

    Uni<AppDO> addAppPermissions(AppDO app, List<PermissionDO> permissions);
    Uni<AppDO> removeAppPermissions(AppDO app, List<PermissionDO> permissions);
}
