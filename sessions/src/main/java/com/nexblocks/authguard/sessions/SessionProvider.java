package com.nexblocks.authguard.sessions;

import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.SessionsService;
import com.nexblocks.authguard.service.auth.AuthProvider;
import com.nexblocks.authguard.service.config.ConfigParser;
import com.nexblocks.authguard.service.config.SessionsConfig;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.service.model.*;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.stream.Collectors;

public class SessionProvider implements AuthProvider {
    private final SessionsService sessionsService;
    private final SessionsConfig sessionsConfig;

    private final Duration sessionTtl;

    @Inject
    public SessionProvider(final SessionsService sessionsService,
                           @Named("sessions") final ConfigContext sessionsConfig) {
        this.sessionsService = sessionsService;
        this.sessionsConfig = sessionsConfig.asConfigBean(SessionsConfig.class);

        this.sessionTtl = ConfigParser.parseDuration(this.sessionsConfig.getLifeTime());
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        final SessionBO session = SessionBO.builder()
                .accountId(account.getId())
                .expiresAt(ZonedDateTime.now().plus(sessionTtl))
                .data(Map.ofEntries(
                        Map.entry(SessionKeys.ACCOUNT_ID, account.getId()),
                        Map.entry(SessionKeys.ROLES, String.join(",", account.getRoles())),
                        Map.entry(SessionKeys.PERMISSIONS, account.getPermissions().stream()
                                .map(Permission::getFullName)
                                .collect(Collectors.joining(",")))
                )).build();

        final SessionBO created = sessionsService.create(session);

        return TokensBO.builder()
                .type("session")
                .token(created.getSessionToken())
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
                .build();
    }

    @Override
    public TokensBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("Sessions cannot be generated for applications");
    }
}
