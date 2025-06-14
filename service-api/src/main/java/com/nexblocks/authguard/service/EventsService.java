package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.EventBO;

import java.time.Instant;
import java.util.List;
import io.smallrye.mutiny.Uni;

public interface EventsService extends CrudService<EventBO> {
    Uni<List<EventBO>> getByDomain(String domain, Instant cursor);
    Uni<List<EventBO>> getByDomainAndChannel(String domain, String channel, Instant cursor);
}
