package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.EventBO;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface EventsService extends CrudService<EventBO> {
    CompletableFuture<List<EventBO>> getByDomain(String domain, Instant cursor);
    CompletableFuture<List<EventBO>> getByDomainAndChannel(String domain, String channel, Instant cursor);
}
