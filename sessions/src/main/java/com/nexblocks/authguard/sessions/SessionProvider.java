package com.nexblocks.authguard.sessions;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.SessionsService;
import com.nexblocks.authguard.service.auth.AuthProvider;
import com.nexblocks.authguard.service.auth.ProvidesToken;
import com.nexblocks.authguard.service.config.ConfigParser;
import com.nexblocks.authguard.service.config.SessionsConfig;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ProvidesToken("sessionToken")
public class SessionProvider implements AuthProvider {
    private static final String TOKEN_TYPE = "session_token";

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
    public AuthResponseBO generateToken(final AccountBO account) {
        return generateToken(account, (TokenOptionsBO) null);
    }

    @Override
    public AuthResponseBO generateToken(final AccountBO account, final TokenOptionsBO options) {
        final SessionBO session = SessionBO.builder()
                .accountId(account.getId())
                .expiresAt(OffsetDateTime.now().plus(sessionTtl))
                .data(Map.ofEntries(
                        Map.entry(SessionKeys.ACCOUNT_ID, account.getId()),
                        Map.entry(SessionKeys.ROLES, String.join(",", account.getRoles())),
                        Map.entry(SessionKeys.PERMISSIONS, account.getPermissions().stream()
                                .map(Permission::getFullName)
                                .collect(Collectors.joining(","))),
                        Map.entry(SessionKeys.SOURCE, Optional.ofNullable(options)
                                .map(TokenOptionsBO::getSource)
                                .orElse(""))
                )).build();

        final SessionBO created = sessionsService.create(session);

        return AuthResponseBO.builder()
                .type(TOKEN_TYPE)
                .token(created.getSessionToken())
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
                .build();
    }

    @Override
    public AuthResponseBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("Sessions cannot be generated for applications");
    }

    @Override
    public AuthResponseBO delete(final AuthRequestBO authRequest) {
        return sessionsService.deleteByToken(authRequest.getToken())
                .map(session -> AuthResponseBO.builder()
                        .type(TOKEN_TYPE)
                        .entityType(EntityType.ACCOUNT)
                        .entityId(session.getAccountId())
                        .token(session.getSessionToken())
                        .build())
                .orElseThrow(() -> new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid session token"));
    }
}
