package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.model.EventDO;
import com.nexblocks.authguard.dal.persistence.EventsRepository;
import com.nexblocks.authguard.dal.persistence.Page;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.EventsService;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.EventBO;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import io.smallrye.mutiny.Uni;
import java.util.stream.Collectors;

public class EventsServiceImpl implements EventsService {
    private static final int PAGE_SIZE = 100;
    private static final Instant DEFAULT_CURSOR = Instant.now().plus(Duration.ofDays(100 * 365));

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
    public Uni<EventBO> create(EventBO event) {
        return persistenceService.create(event);
    }

    @Override
    public Uni<Optional<EventBO>> getById(final long id, final String domain) {
        return persistenceService.getById(id)
                .map(opt -> opt.filter(client -> Objects.equals(client.getDomain(), domain)));
    }

    @Override
    public Uni<Optional<EventBO>> update(final EventBO entity, final String domain) {
        throw new UnsupportedOperationException("Events cannot be updated");
    }

    @Override
    public Uni<Optional<EventBO>> delete(final long id, final String domain) {
        throw new UnsupportedOperationException("Events cannot be deleted");
    }

    @Override
    public Uni<List<EventBO>> getByDomain(final String domain, final Instant cursor) {
        return eventsRepository.findByDomainDescending(domain, Page.of(cursor, PAGE_SIZE, DEFAULT_CURSOR))
                .map(list -> list.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList()));
    }

    @Override
    public Uni<List<EventBO>> getByDomainAndChannel(final String domain,
                                                                  final String channel,
                                                                  final Instant cursor) {
        return eventsRepository.findByDomainAndChannelDescending(domain, channel, Page.of(cursor, PAGE_SIZE, DEFAULT_CURSOR))
                .map(list -> list.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList()));
    }
}
