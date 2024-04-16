package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.model.EventDO;
import com.nexblocks.authguard.dal.persistence.EventsRepository;
import com.nexblocks.authguard.dal.persistence.Page;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.EventsService;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.EventBO;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class EventsServiceImpl implements EventsService {
    private static final int PAGE_SIZE = 100;
    private static final Instant DEFAULT_CURSOR = Instant.MAX;

    private final EventsRepository eventsRepository;
    private final ServiceMapper serviceMapper;

    private final PersistenceService<EventBO, EventDO, EventsRepository> persistenceService;

    @Inject
    public EventsServiceImpl(final MessageBus messageBus,
                             final EventsRepository eventsRepository,
                             final ServiceMapper serviceMapper) {
        this.eventsRepository = eventsRepository;
        this.serviceMapper = serviceMapper;

        this.persistenceService = new PersistenceService<>(eventsRepository, messageBus,
                serviceMapper::toDO, serviceMapper::toBO, null);
    }

    @Override
    public CompletableFuture<EventBO> create(EventBO event) {
        return persistenceService.create(event);
    }

    @Override
    public CompletableFuture<Optional<EventBO>> getById(final long id, final String domain) {
        return persistenceService.getById(id)
                .thenApply(opt -> opt.filter(client -> Objects.equals(client.getDomain(), domain)));
    }

    @Override
    public CompletableFuture<Optional<EventBO>> update(final EventBO entity, final String domain) {
        throw new UnsupportedOperationException("Events cannot be updated");
    }

    @Override
    public CompletableFuture<Optional<EventBO>> delete(final long id, final String domain) {
        throw new UnsupportedOperationException("Events cannot be deleted");
    }

    @Override
    public CompletableFuture<List<EventBO>> getByDomain(final String domain, final Instant cursor) {
        return eventsRepository.findByDomainDescending(domain, Page.of(cursor, PAGE_SIZE, DEFAULT_CURSOR))
                .thenApply(list -> list.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<List<EventBO>> getByDomainAndChannel(final String domain,
                                                                  final String channel,
                                                                  final Instant cursor) {
        return eventsRepository.findByDomainAndChannelDescending(domain, channel, Page.of(cursor, PAGE_SIZE, DEFAULT_CURSOR))
                .thenApply(list -> list.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList()));
    }
}
