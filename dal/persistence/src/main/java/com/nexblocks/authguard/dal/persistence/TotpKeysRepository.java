package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.TotpKeyDO;
import com.nexblocks.authguard.dal.repository.Repository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface TotpKeysRepository extends Repository<TotpKeyDO> {
    CompletableFuture<List<TotpKeyDO>> findByAccountId(String domain, long accountId);
}
