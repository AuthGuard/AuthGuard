package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.basic.config.PasswordlessConfig;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.ExchangeService;
import com.nexblocks.authguard.service.PasswordlessService;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.TokensBO;
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
