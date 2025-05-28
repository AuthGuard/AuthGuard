package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.cache.SessionsRepository;
import com.nexblocks.authguard.dal.model.SessionDO;
import com.nexblocks.authguard.service.TrackingSessionsService;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.Account;
import com.nexblocks.authguard.service.model.Session;
import com.nexblocks.authguard.service.model.SessionBO;
import com.nexblocks.authguard.service.random.CryptographicRandom;
import com.nexblocks.authguard.service.util.ID;
import io.smallrye.mutiny.Uni;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import io.smallrye.mutiny.Uni;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TrackingSessionsServiceImpl implements TrackingSessionsService {
    private static final int KEY_SIZE = 24;
    private static final Duration EXPIRY = Duration.ofDays(365);

    private final SessionsRepository sessionsRepository;
    private final ServiceMapper serviceMapper;
    private final CryptographicRandom cryptographicRandom;

    @Inject
    public TrackingSessionsServiceImpl(final SessionsRepository sessionsRepository, final ServiceMapper serviceMapper) {
        this.sessionsRepository = sessionsRepository;
        this.serviceMapper = serviceMapper;

        this.cryptographicRandom = new CryptographicRandom();
    }

    @Override
    public Uni<Optional<Session>> getByToken(final String token) {
        return sessionsRepository.getByToken(token)
                .map(opt -> opt.map(serviceMapper::toBO)
                        .map(Session.class::cast));
    }

    @Override
    public Uni<Boolean> isSessionActive(final String token, final String domain) {
        return sessionsRepository.getByToken(token)
                .map(opt -> {
                    if (opt.isEmpty()) {
                        return false;
                    }

                    SessionDO session = opt.get();

                    return session.isActive();
                });
    }

    @Override
    public Uni<List<Session>> getByAccountId(final long accountId, final String domain) {
        return sessionsRepository.findByAccountId(accountId, domain)
                .map(list -> list.stream()
                        .map(serviceMapper::toBO)
                        .map(Session.class::cast)
                        .collect(Collectors.toList()));
    }

    @Override
    public Uni<Session> startSession(Account account) {
        SessionBO session = SessionBO.builder()
                .domain(account.getDomain())
                .id(ID.generate())
                .sessionToken(cryptographicRandom.base64Url(KEY_SIZE))
                .expiresAt(Instant.now().plus(EXPIRY))
                .accountId(account.getId())
                .forTracking(true)
                .active(true)
                .build();

        return sessionsRepository.save(serviceMapper.toDO(session))
                .map(serviceMapper::toBO);
    }

    @Override
    public Uni<Session> startAnonymous(final String domain) {
        SessionBO session = SessionBO.builder()
                .domain(domain)
                .id(ID.generate())
                .sessionToken(cryptographicRandom.base64Url(KEY_SIZE))
                .expiresAt(Instant.now().plus(EXPIRY))
                .forTracking(true)
                .active(true)
                .build();

        return sessionsRepository.save(serviceMapper.toDO(session))
                .map(serviceMapper::toBO);
    }

    @Override
    public Uni<Optional<Session>> terminateSession(final String sessionToken, final String domain) {
        return sessionsRepository.getByToken(sessionToken)
                .flatMap(opt -> {
                    if (opt.isPresent() && Objects.equals(opt.get().getDomain(), domain)) {
                        SessionDO session = opt.get();
                        session.setActive(false);

                        return sessionsRepository.save(session)
                                .map(serviceMapper::toBO)
                                .map(Session.class::cast)
                                .map(Optional::of);
                    }

                    Uni<Optional<Session>> em = Uni.createFrom().item(Optional.empty());

                    return em;
                });
    }
}
