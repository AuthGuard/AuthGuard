package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.model.ExchangeAttemptDO;
import com.nexblocks.authguard.dal.persistence.ExchangeAttemptsRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.ExchangeAttemptsService;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.ExchangeAttemptBO;
import com.nexblocks.authguard.service.model.ExchangeAttemptsQueryBO;
import com.google.inject.Inject;
import io.smallrye.mutiny.Uni;

import java.util.Collection;
import java.util.Optional;
import io.smallrye.mutiny.Uni;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ExchangeAttemptsServiceImpl implements ExchangeAttemptsService {
    private static final String EXCHANGE_ATTEMPTS_CHANNEL = "exchange_attempts";

    private final ExchangeAttemptsRepository exchangeAttemptsRepository;
    private final ServiceMapper serviceMapper;
    private final PersistenceService<ExchangeAttemptBO, ExchangeAttemptDO, ExchangeAttemptsRepository> persistenceService;

    @Inject
    public ExchangeAttemptsServiceImpl(final ExchangeAttemptsRepository exchangeAttemptsRepository,
                                       final ServiceMapper serviceMapper,
                                       final MessageBus messageBus) {
        this.exchangeAttemptsRepository = exchangeAttemptsRepository;
        this.serviceMapper = serviceMapper;

        this.persistenceService = new PersistenceService<>(exchangeAttemptsRepository, messageBus, serviceMapper::toDO,
                serviceMapper::toBO, EXCHANGE_ATTEMPTS_CHANNEL);
    }

    @Override
    public Uni<ExchangeAttemptBO> create(final ExchangeAttemptBO entity) {
        return persistenceService.create(entity);
    }

    @Override
    public Uni<Optional<ExchangeAttemptBO>> getById(final long id, String domain) {
        return persistenceService.getById(id);
    }

    @Override
    public Uni<Optional<ExchangeAttemptBO>> update(final ExchangeAttemptBO entity, String domain) {
        throw new UnsupportedOperationException("Exchange attempts cannot be updated");
    }

    @Override
    public Uni<Optional<ExchangeAttemptBO>> delete(final long id, String domain) {
        throw new UnsupportedOperationException("Exchange attempts cannot be deleted");
    }

    @Override
    public Collection<ExchangeAttemptBO> getByEntityId(final long entityId) {
        return exchangeAttemptsRepository.findByEntity(entityId)
                .map(collection -> collection.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList())
                ).subscribeAsCompletionStage().join();
    }

    @Override
    public Collection<ExchangeAttemptBO> find(final ExchangeAttemptsQueryBO query) {
        /*
         * The only three options are:
         * 1. only entityId is specified
         * 2. entityId and fromTimestamp are specified but fromExchange is null
         * 3. all fields are specified
         */
        if (query.getFromTimestamp() == null) {
            return doFind(() -> exchangeAttemptsRepository.findByEntity(query.getEntityId()));
        }

        if (query.getFromExchange() == null) {
            return doFind(() -> exchangeAttemptsRepository
                    .findByEntityAndTimestamp(query.getEntityId(), query.getFromTimestamp()));
        }

        return doFind(() -> exchangeAttemptsRepository
                .findByEntityAndTimestampAndExchange(query.getEntityId(), query.getFromTimestamp(), query.getFromExchange()));
    }

    private Collection<ExchangeAttemptBO> doFind(
            final Supplier<Uni<Collection<ExchangeAttemptDO>>> supplier) {
        return supplier.get()
                .map(collection -> collection.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList())
                ).subscribeAsCompletionStage().join();
    }
}
