package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.SessionsRepository;
import com.nexblocks.authguard.dal.model.SessionDO;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.emb.Messages;
import com.nexblocks.authguard.service.SessionsService;
import com.nexblocks.authguard.service.config.SessionsConfig;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.SessionBO;
import com.nexblocks.authguard.service.random.CryptographicRandom;
import com.nexblocks.authguard.service.util.ID;
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
        sessionDO.setSessionToken(cryptographicRandom.base64Url(config.getRandomSize()));

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

    @Override
    public Optional<SessionBO> deleteByToken(final String token) {
        return sessionsRepository.deleteByToken(token)
                .thenApply(opt -> opt.map(serviceMapper::toBO))
                .join();
    }
}
