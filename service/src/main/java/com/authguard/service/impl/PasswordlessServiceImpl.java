package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.service.ExchangeService;
import com.authguard.service.PasswordlessService;
import com.authguard.service.config.PasswordlessConfig;
import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class PasswordlessServiceImpl implements PasswordlessService {
    private final ExchangeService exchangeService;
    private final PasswordlessConfig passwordlessConfig;

    @Inject
    public PasswordlessServiceImpl(final ExchangeService exchangeService,
                                   @Named("passwordless") final ConfigContext passwordlessConfig) {
        this.exchangeService = exchangeService;
        this.passwordlessConfig = passwordlessConfig.asConfigBean(PasswordlessConfig.class);
    }

    @Override
    public TokensBO authenticate(final AuthRequestBO authRequest) {
        return exchangeService.exchange(authRequest, "passwordless", passwordlessConfig.getGenerateToken());
    }

    @Override
    public TokensBO authenticate(final String passwordlessToken) {
        return authenticate(AuthRequestBO.builder().token(passwordlessToken).build());
    }
}
