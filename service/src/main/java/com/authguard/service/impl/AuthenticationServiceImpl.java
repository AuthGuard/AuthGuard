package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.emb.MessageBus;
import com.authguard.emb.model.EventType;
import com.authguard.emb.model.Message;
import com.authguard.service.*;
import com.authguard.service.config.AuthenticationConfig;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.authguard.service.model.TokensBO;

import java.time.OffsetDateTime;
import java.util.Optional;

public class AuthenticationServiceImpl implements AuthenticationService {
    private static final String FROM_TOKEN_TYPE = "basic";
    private static final String AUTH_CHANNEL = "auth";

    private final ExchangeService exchangeService;
    private final String generateTokenType;
    private final MessageBus messageBus;

    @Inject
    public AuthenticationServiceImpl(final ExchangeService exchangeService,
                                     final MessageBus messageBus,
                                     final @Named("authentication") ConfigContext authenticationConfig) {
        this.messageBus = messageBus;
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
        // it hasn't been decided what the message should contain
        messageBus.publish(AUTH_CHANNEL, Message.builder()
                .eventType(EventType.AUTHENTICATION)
                .timestamp(OffsetDateTime.now())
                .bodyType(String.class)
                .messageBody("")
                .build()
        );

        return Optional.of(exchangeService.exchange(header, FROM_TOKEN_TYPE, generateTokenType));
    }

}
