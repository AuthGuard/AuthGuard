package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.EventDO;
import com.nexblocks.authguard.dal.repository.ImmutableRecordRepository;
import com.nexblocks.authguard.dal.repository.IndelibleRecordRepository;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface EventsRepository
        extends ImmutableRecordRepository<EventDO>, IndelibleRecordRepository<EventDO> {
    CompletableFuture<List<EventDO>> findByDomainDescending(String domain, Page<Instant> page);
    CompletableFuture<List<EventDO>> findByDomainAndChannelDescending(String domain, String channel,
                                                                      Page<Instant> page);
}
