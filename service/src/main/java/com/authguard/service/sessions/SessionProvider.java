package com.authguard.service.sessions;

import com.authguard.config.ConfigContext;
import com.authguard.dal.SessionsRepository;
import com.authguard.service.auth.AuthProvider;
import com.authguard.service.config.ConfigParser;
import com.authguard.service.config.SessionsConfig;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.SessionBO;
import com.authguard.service.model.TokensBO;
import com.authguard.service.random.CryptographicRandom;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.time.ZonedDateTime;

public class SessionProvider implements AuthProvider {
    private final SessionsRepository sessionsRepository;
    private final SessionsConfig sessionsConfig;
    private final ServiceMapper serviceMapper;

    private final CryptographicRandom random;

    @Inject
    public SessionProvider(final SessionsRepository sessionsRepository,
                           @Named("sessions") final ConfigContext sessionsConfig,
                           final ServiceMapper serviceMapper) {
        this.sessionsRepository = sessionsRepository;
        this.sessionsConfig = sessionsConfig.asConfigBean(SessionsConfig.class);
        this.serviceMapper = serviceMapper;

        this.random = new CryptographicRandom();
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        final SessionBO session = SessionBO.builder()
                .id(random.base64(sessionsConfig.getRandomSize()))
                .accountId(account.getId())
                .expiresAt(ZonedDateTime.now().plus(ConfigParser.parseDuration(sessionsConfig.getLifeTime())))
                .build();

        return sessionsRepository.save(serviceMapper.toDO(session))
                .thenApply(persisted -> TokensBO.builder()
                        .type("session")
                        .token(persisted.getId())
                        .build()
                ).join();
    }

    @Override
    public TokensBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("Sessions cannot be generated for applications");
    }
}
