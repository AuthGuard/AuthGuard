package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.EventDO;
import com.nexblocks.authguard.dal.repository.ImmutableRecordRepository;
import com.nexblocks.authguard.dal.repository.IndelibleRecordRepository;

import java.time.Instant;
import java.util.List;
import io.smallrye.mutiny.Uni;

public interface EventsRepository
        extends ImmutableRecordRepository<EventDO>, IndelibleRecordRepository<EventDO> {
    Uni<List<EventDO>> findByDomainDescending(String domain, Page<Instant> page);
    Uni<List<EventDO>> findByDomainAndChannelDescending(String domain, String channel,
                                                                      Page<Instant> page);
}
