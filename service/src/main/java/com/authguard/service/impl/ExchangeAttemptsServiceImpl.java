package com.authguard.service.impl;

import com.authguard.dal.persistence.ExchangeAttemptsRepository;
import com.authguard.service.ExchangeAttemptsService;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.model.ExchangeAttemptBO;
import com.google.inject.Inject;

import java.util.Collection;
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
}
