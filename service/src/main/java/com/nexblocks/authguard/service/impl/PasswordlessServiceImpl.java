package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.basic.config.PasswordlessConfig;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.ExchangeService;
import com.nexblocks.authguard.service.PasswordlessService;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.model.TokensBO;

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
    public TokensBO authenticate(final AuthRequestBO authRequest, final RequestContextBO requestContext) {
        return exchangeService.exchange(authRequest, "passwordless", passwordlessConfig.getGenerateToken(), requestContext);
    }

    @Override
    public TokensBO authenticate(final String passwordlessToken, final RequestContextBO requestContext) {
        return authenticate(AuthRequestBO.builder().token(passwordlessToken).build(), requestContext);
    }
}
