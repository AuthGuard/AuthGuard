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
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import io.smallrye.mutiny.Uni;
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
    public Uni<AuthResponseBO> generateToken(final AccountBO account, final TokenRestrictionsBO restrictions,
                                                           final TokenOptionsBO options) {
        if (!account.isActive()) {
            throw new ServiceAuthorizationException(ErrorCode.ACCOUNT_INACTIVE, "Account was deactivated");
        }

        final Map<String, String> data = new HashMap<>();

        data.put(SessionKeys.ACCOUNT_ID, "" + account.getId());
        data.put(SessionKeys.ROLES, String.join(",", account.getRoles()));
        data.put(SessionKeys.PERMISSIONS, account.getPermissions().stream()
                .map(Permission::getFullName)
                .collect(Collectors.joining(",")));

        if (options != null) {
            if (options.getSource() != null) {
                data.put(SessionKeys.SOURCE, options.getSource());
            }

            if (options.getUserAgent() != null) {
                data.put(SessionKeys.USER_AGENT, options.getUserAgent());
            }
        }

        final SessionBO session = SessionBO.builder()
                .accountId(account.getId())
                .expiresAt(Instant.now().plus(sessionTtl))
                .data(data)
                .build();

        return sessionsService.create(session)
                .map(created -> AuthResponseBO.builder()
                        .type(TOKEN_TYPE)
                        .token(created.getSessionToken())
                        .entityType(EntityType.ACCOUNT)
                        .entityId(account.getId())
                        .validFor(sessionTtl.getSeconds())
                        .build());
    }

    @Override
    public AuthResponseBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("Sessions cannot be generated for applications");
    }

    @Override
    public Uni<AuthResponseBO> delete(final AuthRequestBO authRequest) {
        return sessionsService.deleteByToken(authRequest.getToken())
                .map(opt -> opt
                        .map(session -> AuthResponseBO.builder()
                                .type(TOKEN_TYPE)
                                .entityType(EntityType.ACCOUNT)
                                .entityId(session.getAccountId())
                                .token(session.getSessionToken())
                                .build())
                        .orElseThrow(() -> new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid session token")));
    }
}
