package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.ApiKeyDO;
import com.nexblocks.authguard.dal.repository.ImmutableRecordRepository;
import io.smallrye.mutiny.Uni;

import java.util.Collection;
import java.util.Optional;

public interface ApiKeysRepository extends ImmutableRecordRepository<ApiKeyDO> {
    Uni<Collection<ApiKeyDO>> getByAppId(long id);
    Uni<Optional<ApiKeyDO>> getByKey(String key);
}
