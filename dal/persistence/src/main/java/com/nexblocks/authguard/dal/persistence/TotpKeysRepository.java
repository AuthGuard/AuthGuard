package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.TotpKeyDO;
import com.nexblocks.authguard.dal.repository.Repository;

import java.util.List;
import io.smallrye.mutiny.Uni;

public interface TotpKeysRepository extends Repository<TotpKeyDO> {
    Uni<List<TotpKeyDO>> findByAccountId(String domain, long accountId);
}
