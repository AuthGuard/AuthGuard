package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.ExchangeAttemptDO;
import com.nexblocks.authguard.dal.repository.ImmutableRecordRepository;
import com.nexblocks.authguard.dal.repository.IndelibleRecordRepository;

import java.time.Instant;
import java.util.Collection;
import io.smallrye.mutiny.Uni;

public interface ExchangeAttemptsRepository
        extends ImmutableRecordRepository<ExchangeAttemptDO>, IndelibleRecordRepository<ExchangeAttemptDO> {
    Uni<Collection<ExchangeAttemptDO>> findByEntity(long entityId);

    Uni<Collection<ExchangeAttemptDO>> findByEntityAndTimestamp(long entityId, Instant fromTimestamp);

    Uni<Collection<ExchangeAttemptDO>> findByEntityAndTimestampAndExchange(long entityId,
                                                                                         Instant fromTimestamp,
                                                                                         String fromExchange);
}
