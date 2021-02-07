package com.authguard.service.impl;

import com.authguard.emb.MessageBus;
import com.authguard.emb.Messages;
import com.authguard.service.ExchangeAttemptsService;
import com.authguard.service.ExchangeService;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.messaging.AuthMessage;
import com.authguard.service.model.*;
import com.google.inject.Inject;
import io.vavr.control.Either;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExchangeServiceImpl implements ExchangeService {
    static final String CHANNEL = "auth";

    private final Map<String, Exchange> exchanges;
    private final ExchangeAttemptsService exchangeAttemptsService;
    private final MessageBus emb;

    @Inject
    public ExchangeServiceImpl(final List<Exchange> exchanges, final ExchangeAttemptsService exchangeAttemptsService,
                               final MessageBus emb) {
        this.exchanges = mapExchanges(exchanges);
        this.exchangeAttemptsService = exchangeAttemptsService;
        this.emb = emb;
    }

    @Override
    public TokensBO exchange(final AuthRequestBO authRequest, final String fromTokenType, final String toTokenType) {
        return exchange(authRequest, null, fromTokenType, toTokenType);
    }

    @Override
    public TokensBO exchange(final AuthRequestBO authRequest, final TokenRestrictionsBO restrictions, final String fromTokenType, final String toTokenType) {
        final String key = exchangeKey(fromTokenType, toTokenType);
        final Exchange exchange = exchanges.get(key);

        if (exchange == null) {
            throw new ServiceException(ErrorCode.UNKNOWN_EXCHANGE, "Unknown token exchange " + fromTokenType + " to " + toTokenType);
        }

        final Either<Exception, TokensBO> result = restrictions == null ?
                exchange.exchange(authRequest) :
                exchange.exchange(authRequest, restrictions);

        if (result.isRight()) {
            final TokensBO tokens = result.get();

            exchangeSuccess(tokens, fromTokenType, toTokenType);

            return tokens;
        } else {
            final Exception e = result.getLeft();

            exchangeFailure(e, fromTokenType, toTokenType);

            // TODO remove this
            if (ServiceException.class.isAssignableFrom(e.getClass())) {
                throw (ServiceException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean supportsExchange(final String fromTokenType, final String toTokenType) {
        return exchanges.containsKey(exchangeKey(fromTokenType, toTokenType));
    }

    private void exchangeSuccess(final TokensBO tokens, final String fromTokenType,
                                 final String toTokenType) {
        final AuthMessage authMessage = AuthMessage.success(fromTokenType, toTokenType,
                tokens.getEntityType(), tokens.getEntityId());

        exchangeAttemptsService.create(ExchangeAttemptBO.builder()
                .entityId(tokens.getEntityId())
                .exchangeFrom(fromTokenType)
                .exchangeTo(toTokenType)
                .successful(true)
                .build());

        emb.publish(CHANNEL, Messages.auth(authMessage));
    }

    private void exchangeFailure(final Exception e, final String fromTokenType,
                                 final String toTokenType) {

        if (ServiceAuthorizationException.class.isAssignableFrom(e.getClass())) {
            final ServiceAuthorizationException sae = (ServiceAuthorizationException) e;

            final AuthMessage authMessage = AuthMessage.failure(fromTokenType, toTokenType,
                    sae.getEntityType(), sae.getEntityId(), sae);

            if (sae.getEntityType() == EntityType.ACCOUNT) {
                exchangeAttemptsService.create(ExchangeAttemptBO.builder()
                        .entityId(sae.getEntityId())
                        .exchangeFrom(fromTokenType)
                        .exchangeTo(toTokenType)
                        .successful(false)
                        .build());
            }

            emb.publish(CHANNEL, Messages.auth(authMessage));
        } else {
            final AuthMessage authMessage = AuthMessage.failure(fromTokenType, toTokenType, e);

            emb.publish(CHANNEL, Messages.auth(authMessage));
        }

    }

    private Map<String, Exchange> mapExchanges(final List<Exchange> exchanges) {
        return exchanges.stream()
                .filter(exchange -> exchange.getClass().getAnnotation(TokenExchange.class) != null)
                .collect(Collectors.toMap(
                        this::tokenExchangeToString,
                        Function.identity()
                ));
    }

    private String tokenExchangeToString(final Exchange exchange) {
        final TokenExchange tokenExchange = exchange.getClass().getAnnotation(TokenExchange.class);
        return exchangeKey(tokenExchange.from(), tokenExchange.to());
    }

    private String exchangeKey(final String fromTokenType, final String toTokenType) {
        return fromTokenType + "-" + toTokenType;
    }
}
