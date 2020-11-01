package com.authguard.sessions;

import com.authguard.config.ConfigContext;
import com.authguard.service.SessionsService;
import com.authguard.service.auth.AuthProvider;
import com.authguard.service.config.ConfigParser;
import com.authguard.service.config.SessionsConfig;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.SessionBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.time.ZonedDateTime;

public class SessionProvider implements AuthProvider {
    private final SessionsService sessionsService;
    private final SessionsConfig sessionsConfig;

    @Inject
    public SessionProvider(final SessionsService sessionsService,
                           @Named("sessions") final ConfigContext sessionsConfig) {
        this.sessionsService = sessionsService;
        this.sessionsConfig = sessionsConfig.asConfigBean(SessionsConfig.class);
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        final SessionBO session = SessionBO.builder()
                .accountId(account.getId())
                .expiresAt(ZonedDateTime.now().plus(ConfigParser.parseDuration(sessionsConfig.getLifeTime())))
                .build();

        final SessionBO created = sessionsService.create(session);

        return TokensBO.builder()
                .type("session")
                .token(created.getSessionToken())
                .build();
    }

    @Override
    public TokensBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("Sessions cannot be generated for applications");
    }
}
