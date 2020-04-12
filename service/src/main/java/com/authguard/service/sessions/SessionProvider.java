package com.authguard.service.sessions;

import com.authguard.config.ConfigContext;
import com.authguard.dal.SessionsRepository;
import com.authguard.service.AuthProvider;
import com.authguard.service.config.ConfigParser;
import com.authguard.service.config.SessionsConfig;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.SessionBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Base64;

public class SessionProvider implements AuthProvider {
    private final SessionsRepository sessionsRepository;
    private final SecureRandom secureRandom;
    private final SessionsConfig sessionsConfig;
    private final ServiceMapper serviceMapper;

    @Inject
    public SessionProvider(final SessionsRepository sessionsRepository,
                           @Named("sessions") final ConfigContext sessionsConfig,
                           final ServiceMapper serviceMapper) {
        this.sessionsRepository = sessionsRepository;
        this.sessionsConfig = sessionsConfig.asConfigBean(SessionsConfig.class);
        this.serviceMapper = serviceMapper;

        this.secureRandom = new SecureRandom();
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        final SessionBO session = SessionBO.builder()
                .id(randomSessionId())
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

    private String randomSessionId() {
        final byte[] bytes = new byte[sessionsConfig.getRandomSize()];

        secureRandom.nextBytes(bytes);

        return Base64.getEncoder().encodeToString(bytes);
    }
}
