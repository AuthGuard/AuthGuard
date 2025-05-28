package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.AccountLocksService;
import com.nexblocks.authguard.service.AuthenticationService;
import com.nexblocks.authguard.service.ExchangeService;
import com.nexblocks.authguard.service.config.AuthenticationConfig;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AccountLockBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

public class AuthenticationServiceImpl implements AuthenticationService {
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    private static final String BASIC_TOKEN_TYPE = "basic";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final ExchangeService exchangeService;
    private final AccountLocksService accountLocksService;
    private final String generateTokenType;
    private final String logoutTokenType;

    @Inject
    public AuthenticationServiceImpl(final ExchangeService exchangeService,
                                     final AccountLocksService accountLocksService,
                                     final @Named("authentication") ConfigContext configContext) {
        this.accountLocksService = accountLocksService;

        final AuthenticationConfig authenticationConfig = configContext.asConfigBean(AuthenticationConfig.class);

        this.generateTokenType = authenticationConfig.getGenerateToken();
        this.logoutTokenType = authenticationConfig.getLogoutToken();

        if (!exchangeService.supportsExchange(BASIC_TOKEN_TYPE, this.generateTokenType)) {
            throw new IllegalArgumentException("Unsupported exchange basic to "
                    + authenticationConfig.getGenerateToken());
        }

        this.exchangeService = exchangeService;
    }

    @Override
    public Uni<AuthResponseBO> authenticate(final AuthRequestBO authRequest, final RequestContextBO requestContext) {
        return exchangeService.exchange(authRequest, BASIC_TOKEN_TYPE, generateTokenType, requestContext)
                .flatMap(tokens ->
                        accountLocksService.getActiveLocksByAccountId(tokens.getEntityId())
                                .map(locks -> {
                                    if (locks == null || locks.isEmpty()) {
                                        return tokens;
                                    }

                                    throw new ServiceAuthorizationException(ErrorCode.ACCOUNT_IS_LOCKED,
                                            "There is an active lock on account " + tokens.getEntityId());
                                }));
    }

    @Override
    public Uni<AuthResponseBO> logout(final AuthRequestBO authRequest, final RequestContextBO requestContext) {
        return exchangeService.delete(authRequest, logoutTokenType);
    }

    @Override
    public Uni<AuthResponseBO> refresh(final AuthRequestBO authRequest, final RequestContextBO requestContext) {
        return exchangeService.exchange(authRequest, REFRESH_TOKEN_TYPE, generateTokenType, requestContext);
    }

}
