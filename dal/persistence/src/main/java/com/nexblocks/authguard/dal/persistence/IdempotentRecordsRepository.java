package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.IdempotentRecordDO;
import com.nexblocks.authguard.dal.repository.ImmutableRecordRepository;
import com.nexblocks.authguard.dal.repository.IndelibleRecordRepository;

import java.util.List;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

public interface IdempotentRecordsRepository
        extends ImmutableRecordRepository<IdempotentRecordDO>, IndelibleRecordRepository<IdempotentRecordDO> {
    Uni<List<IdempotentRecordDO>> findByKey(String idempotentKey);
    Uni<Optional<IdempotentRecordDO>> findByKeyAndEntityType(String idempotentKey, String entityType);
}
