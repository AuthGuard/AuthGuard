package com.authguard.service.impl;

import com.authguard.dal.model.ExchangeAttemptDO;
import com.authguard.dal.persistence.ExchangeAttemptsRepository;
import com.authguard.service.ExchangeAttemptsService;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.model.ExchangeAttemptBO;
import com.authguard.service.model.ExchangeAttemptsQueryBO;
import com.google.inject.Inject;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ExchangeAttemptsServiceImpl implements ExchangeAttemptsService {
    private final ExchangeAttemptsRepository exchangeAttemptsRepository;
    private final ServiceMapper serviceMapper;

    @Inject
    public ExchangeAttemptsServiceImpl(final ExchangeAttemptsRepository exchangeAttemptsRepository,
                                       final ServiceMapper serviceMapper) {
        this.exchangeAttemptsRepository = exchangeAttemptsRepository;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public Collection<ExchangeAttemptBO> get(final String entityId) {
        return exchangeAttemptsRepository.findByEntity(entityId)
                .thenApply(collection -> collection.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList())
                ).join();
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
            final Supplier<CompletableFuture<Collection<ExchangeAttemptDO>>> supplier) {
        return supplier.get()
                .thenApply(collection -> collection.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList())
                ).join();
    }
}
