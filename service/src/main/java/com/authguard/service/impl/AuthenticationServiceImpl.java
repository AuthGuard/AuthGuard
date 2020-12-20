package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.service.AccountLocksService;
import com.authguard.service.AuthenticationService;
import com.authguard.service.ExchangeService;
import com.authguard.service.config.AuthenticationConfig;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.model.AccountLockBO;
import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.Collection;
import java.util.Optional;

public class AuthenticationServiceImpl implements AuthenticationService {
    private static final String FROM_TOKEN_TYPE = "basic";

    private final ExchangeService exchangeService;
    private final AccountLocksService accountLocksService;
    private final String generateTokenType;

    @Inject
    public AuthenticationServiceImpl(final ExchangeService exchangeService,
                                     final AccountLocksService accountLocksService,
                                     final @Named("authentication") ConfigContext configContext) {
        this.accountLocksService = accountLocksService;

        final AuthenticationConfig authenticationConfig = configContext.asConfigBean(AuthenticationConfig.class);

        this.generateTokenType = authenticationConfig.getGenerateToken();

        if (!exchangeService.supportsExchange(FROM_TOKEN_TYPE, this.generateTokenType)) {
            throw new IllegalArgumentException("Unsupported exchange basic to "
                    + authenticationConfig.getGenerateToken());
        }

        this.exchangeService = exchangeService;
    }

    @Override
    public Optional<TokensBO> authenticate(final AuthRequestBO authRequest) {
        final TokensBO tokens = exchangeService.exchange(authRequest, FROM_TOKEN_TYPE, generateTokenType);
        final Collection<AccountLockBO> locks = accountLocksService.getActiveLocksByAccountId(tokens.getEntityId());

        if (locks == null || locks.isEmpty()) {
            return Optional.of(tokens);
        } else {
            throw new ServiceAuthorizationException(ErrorCode.ACCOUNT_IS_LOCKED,
                    "There is an active lock on account " + tokens.getEntityId());
        }
    }

}
