package com.authguard.dal;

import com.authguard.dal.model.ExchangeAttemptDO;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface ExchangeAttemptsRepository {
    CompletableFuture<ExchangeAttemptDO> save(ExchangeAttemptDO loginAttempt);

    CompletableFuture<Collection<ExchangeAttemptDO>> findByEntity(String entityId);

    CompletableFuture<Collection<ExchangeAttemptDO>> findByEntityAndTimestamp(String entityId, LocalDateTime fromTimestamp);

    CompletableFuture<Collection<ExchangeAttemptDO>> findByEntityAndTimestampAndExchange(String entityId,
                                                                                         LocalDateTime fromTimestamp,
                                                                                         String fromExchange);
}
