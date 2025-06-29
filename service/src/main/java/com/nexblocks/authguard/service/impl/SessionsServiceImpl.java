package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
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

import java.util.Optional;
import io.smallrye.mutiny.Uni;

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
    public Uni<SessionBO> create(final SessionBO session) {
        final SessionDO sessionDO = serviceMapper.toDO(session);

        sessionDO.setId(ID.generate());
        sessionDO.setSessionToken(cryptographicRandom.base64Url(config.getRandomSize()));

        return sessionsRepository.save(sessionDO)
                .map(created -> {
                    emb.publish(CHANNEL, Messages.created(created, null));
                    return serviceMapper.toBO(created);
                });
    }

    @Override
    public Uni<Optional<SessionBO>> getById(final long id) {
        return sessionsRepository.getById(id)
                .map(opt -> opt.map(serviceMapper::toBO));
    }

    @Override
    public Uni<Optional<SessionBO>> getByToken(final String token) {
        return sessionsRepository.getByToken(token)
                .map(opt -> opt.map(serviceMapper::toBO));
    }

    @Override
    public Uni<Optional<SessionBO>> deleteByToken(final String token) {
        return sessionsRepository.deleteByToken(token)
                .map(opt -> opt.map(serviceMapper::toBO));
    }
}
