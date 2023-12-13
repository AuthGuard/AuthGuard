package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.ExchangeAttemptDO;
import com.nexblocks.authguard.dal.repository.ImmutableRecordRepository;
import com.nexblocks.authguard.dal.repository.IndelibleRecordRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface ExchangeAttemptsRepository
        extends ImmutableRecordRepository<ExchangeAttemptDO>, IndelibleRecordRepository<ExchangeAttemptDO> {
    CompletableFuture<Collection<ExchangeAttemptDO>> findByEntity(long entityId);

    CompletableFuture<Collection<ExchangeAttemptDO>> findByEntityAndTimestamp(long entityId, Instant fromTimestamp);

    CompletableFuture<Collection<ExchangeAttemptDO>> findByEntityAndTimestampAndExchange(long entityId,
                                                                                         Instant fromTimestamp,
                                                                                         String fromExchange);
}
