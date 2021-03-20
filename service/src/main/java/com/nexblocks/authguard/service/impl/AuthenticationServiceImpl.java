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
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.model.TokensBO;

import java.util.Collection;
import java.util.Optional;

public class AuthenticationServiceImpl implements AuthenticationService {
    private static final String FROM_TOKEN_TYPE = "basic";

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

        if (!exchangeService.supportsExchange(FROM_TOKEN_TYPE, this.generateTokenType)) {
            throw new IllegalArgumentException("Unsupported exchange basic to "
                    + authenticationConfig.getGenerateToken());
        }

        this.exchangeService = exchangeService;
    }

    @Override
    public Optional<TokensBO> authenticate(final AuthRequestBO authRequest, final RequestContextBO requestContext) {
        final TokensBO tokens = exchangeService.exchange(authRequest, FROM_TOKEN_TYPE, generateTokenType, requestContext);
        final Collection<AccountLockBO> locks = accountLocksService.getActiveLocksByAccountId(tokens.getEntityId());

        if (locks == null || locks.isEmpty()) {
            return Optional.of(tokens);
        } else {
            throw new ServiceAuthorizationException(ErrorCode.ACCOUNT_IS_LOCKED,
                    "There is an active lock on account " + tokens.getEntityId());
        }
    }

    @Override
    public Optional<TokensBO> logout(final AuthRequestBO authRequest, final RequestContextBO requestContext) {
        return Optional.of(exchangeService.delete(authRequest, logoutTokenType));
    }

}
