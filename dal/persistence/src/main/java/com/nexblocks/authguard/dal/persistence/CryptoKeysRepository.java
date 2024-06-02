package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.CryptoKeyDO;
import com.nexblocks.authguard.dal.repository.Repository;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CryptoKeysRepository extends Repository<CryptoKeyDO> {
    CompletableFuture<List<CryptoKeyDO>> findByDomain(String domain, Page<Instant> page);
    CompletableFuture<List<CryptoKeyDO>> findByAccountId(String domain, long accountId, Page<Instant> page);
    CompletableFuture<List<CryptoKeyDO>> findByAppId(String domain, long appId, Page<Instant> page);
}
