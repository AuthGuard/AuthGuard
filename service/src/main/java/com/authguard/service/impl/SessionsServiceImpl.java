package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.dal.SessionsRepository;
import com.authguard.dal.model.SessionDO;
import com.authguard.emb.MessageBus;
import com.authguard.emb.Messages;
import com.authguard.service.SessionsService;
import com.authguard.service.config.SessionsConfig;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.model.SessionBO;
import com.authguard.service.random.CryptographicRandom;
import com.authguard.service.util.ID;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.Optional;

public class SessionsServiceImpl implements SessionsService {
    private final String CHANNEL = "sessions";

    private final SessionsRepository sessionsRepository;
    private final MessageBus emb;
    private final ServiceMapper serviceMapper;
    private final SessionsConfig config;
    private final CryptographicRandom cryptographicRandom;

    @Inject
    public SessionsServiceImpl(final SessionsRepository sessionsRepository,
                               final MessageBus emb,
                               final ServiceMapper serviceMapper,
                               final @Named("sessions") ConfigContext config) {
        this(sessionsRepository, emb, serviceMapper, config.asConfigBean(SessionsConfig.class));
    }

    public SessionsServiceImpl(final SessionsRepository sessionsRepository,
                               final MessageBus emb,
                               final ServiceMapper serviceMapper,
                               final SessionsConfig config) {
        this.sessionsRepository = sessionsRepository;
        this.emb = emb;
        this.serviceMapper = serviceMapper;
        this.config = config;

        this.cryptographicRandom = new CryptographicRandom();
    }

    @Override
    public SessionBO create(final SessionBO session) {
        final SessionDO sessionDO = serviceMapper.toDO(session);

        sessionDO.setId(ID.generate());
        sessionDO.setSessionToken(cryptographicRandom.base64(config.getRandomSize()));

        return sessionsRepository.save(sessionDO)
                .thenApply(created -> {
                    emb.publish(CHANNEL, Messages.created(created));
                    return serviceMapper.toBO(created);
                }).join();
    }

    @Override
    public Optional<SessionBO> getById(final String id) {
        return sessionsRepository.getById(id)
                .thenApply(opt -> opt.map(serviceMapper::toBO))
                .join();
    }

    @Override
    public Optional<SessionBO> getByToken(final String token) {
        return sessionsRepository.getByToken(token)
                .thenApply(opt -> opt.map(serviceMapper::toBO))
                .join();
    }
}