package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.ClientDO;
import com.nexblocks.authguard.dal.repository.Repository;

import java.util.List;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

public interface ClientsRepository extends Repository<ClientDO> {
    Uni<Optional<ClientDO>> getByExternalId(String externalId);
    Uni<List<ClientDO>> getAllForAccount(long accountId, Page<Long> page);
    Uni<List<ClientDO>> getByType(String type, Page<Long> page);
    Uni<List<ClientDO>> getByDomain(String domain, Page<Long> page);
}
