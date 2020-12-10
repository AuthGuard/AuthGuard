package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.service.AuthenticationService;
import com.authguard.service.ExchangeService;
import com.authguard.service.config.AuthenticationConfig;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.Optional;

public class AuthenticationServiceImpl implements AuthenticationService {
    private static final String FROM_TOKEN_TYPE = "basic";

    private final ExchangeService exchangeService;
    private final String generateTokenType;

    @Inject
    public AuthenticationServiceImpl(final ExchangeService exchangeService,
                                     final @Named("authentication") ConfigContext authenticationConfig) {
        final AuthenticationConfig authenticationConfig1 = authenticationConfig.asConfigBean(AuthenticationConfig.class);
        this.generateTokenType = authenticationConfig1.getGenerateToken();

        if (!exchangeService.supportsExchange(FROM_TOKEN_TYPE, this.generateTokenType)) {
            throw new IllegalArgumentException("Unsupported exchange basic to "
                    + authenticationConfig1.getGenerateToken());
        }

        this.exchangeService = exchangeService;
    }

    @Override
    public Optional<TokensBO> authenticate(final String header) {
        return Optional.of(exchangeService.exchange(header, FROM_TOKEN_TYPE, generateTokenType));
    }

}
