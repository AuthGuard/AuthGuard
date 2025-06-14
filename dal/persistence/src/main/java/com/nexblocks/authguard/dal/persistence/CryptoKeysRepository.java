package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.CryptoKeyDO;
import com.nexblocks.authguard.dal.repository.Repository;

import java.time.Instant;
import java.util.List;
import io.smallrye.mutiny.Uni;

public interface CryptoKeysRepository extends Repository<CryptoKeyDO> {
    Uni<List<CryptoKeyDO>> findByDomain(String domain, Page<Instant> page);
    Uni<List<CryptoKeyDO>> findByAccountId(String domain, long accountId, Page<Instant> page);
    Uni<List<CryptoKeyDO>> findByAppId(String domain, long appId, Page<Instant> page);
}
