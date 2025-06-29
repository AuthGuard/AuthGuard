package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.emb.Messages;
import com.nexblocks.authguard.service.ExchangeAttemptsService;
import com.nexblocks.authguard.service.ExchangeService;
import com.nexblocks.authguard.service.auth.AuthProvider;
import com.nexblocks.authguard.service.auth.ProvidesToken;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.messaging.AuthMessage;
import com.nexblocks.authguard.service.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import io.smallrye.mutiny.Uni;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExchangeServiceImpl implements ExchangeService {
    private final static Logger LOG = LoggerFactory.getLogger(ExchangeServiceImpl.class);

    static final String CHANNEL = "auth";

    private final Map<String, Exchange> exchanges;
    private final Map<String, AuthProvider> authProviders;
    private final ExchangeAttemptsService exchangeAttemptsService;
    private final MessageBus emb;

    @Inject
    public ExchangeServiceImpl(final List<Exchange> exchanges, final List<AuthProvider> authProviders,
                               final ExchangeAttemptsService exchangeAttemptsService,
                               final MessageBus emb) {
        this.exchanges = mapExchanges(exchanges);
        this.authProviders = mapProviders(authProviders);
        this.exchangeAttemptsService = exchangeAttemptsService;
        this.emb = emb;
    }

    @Override
    public Uni<AuthResponseBO> exchange(final AuthRequestBO authRequest, final String fromTokenType, final String toTokenType,
                                   final RequestContextBO requestContext) {
        final String key = exchangeKey(fromTokenType, toTokenType);
        final Exchange exchange = exchanges.get(key);

        if (exchange == null) {
            LOG.warn("A request was made for an unknown exchange. fromTokenType={}, toTokenType={}",
                    fromTokenType, toTokenType);

            throw new ServiceException(ErrorCode.UNKNOWN_EXCHANGE, "Unknown token exchange " + fromTokenType + " to " + toTokenType);
        }

        AuthRequestBO enrichedRequest = AuthRequestBO.builder()
                .from(authRequest)
                .userAgent(requestContext.getUserAgent())
                .clientId(requestContext.getClientId())
                .sourceIp(requestContext.getSource())
                .build();

        return exchange.exchange(enrichedRequest)
                .onFailure()
                .invoke(e -> {
                    LOG.info("Unsuccessful exchange. from={}, to={}, request={}, error={}", fromTokenType, toTokenType,
                            authRequest, e.getMessage());

                    exchangeFailure(authRequest, requestContext, e, fromTokenType, toTokenType);
                })
                .map(tokens -> {
                    LOG.info("Successful exchange. from={}, to={}, request={}", fromTokenType, toTokenType, authRequest);

                    exchangeSuccess(authRequest, requestContext, tokens, fromTokenType, toTokenType);

                    return tokens;
                });
    }

    @Override
    public boolean supportsExchange(final String fromTokenType, final String toTokenType) {
        return exchanges.containsKey(exchangeKey(fromTokenType, toTokenType));
    }

    @Override
    public Uni<AuthResponseBO> delete(final AuthRequestBO authRequest, final String tokenType) {
        AuthProvider provider = authProviders.get(tokenType);

        if (provider == null) {
            throw new ServiceException(ErrorCode.UNKNOWN_EXCHANGE, "Unknown token type " + tokenType);
        }

        return provider.delete(authRequest);
    }

    private void exchangeSuccess(final AuthRequestBO authRequest, final RequestContextBO requestContext,
                                 final AuthResponseBO tokens, final String fromTokenType, final String toTokenType) {
        AuthMessage authMessage = AuthMessage.success(fromTokenType, toTokenType,
                tokens.getEntityType(), tokens.getEntityId());

        ExchangeAttemptBO attempt = createBaseAttempt(authRequest, requestContext)
                .exchangeFrom(fromTokenType)
                .exchangeTo(toTokenType)
                .successful(true)
                .entityId(tokens.getEntityId())
                .build();

        exchangeAttemptsService.create(attempt);

        emb.publish(CHANNEL, Messages.auth(authMessage, authRequest.getDomain()));
    }

    private void exchangeFailure(final AuthRequestBO authRequest, final RequestContextBO requestContext,
                                 final Throwable e, final String fromTokenType, final String toTokenType) {

        if (ServiceAuthorizationException.class.isAssignableFrom(e.getClass())) {
            ServiceAuthorizationException sae = (ServiceAuthorizationException) e;

            AuthMessage authMessage = AuthMessage.failure(fromTokenType, toTokenType,
                    sae.getEntityType(), sae.getEntityId(), sae);

            if (sae.getEntityType() == EntityType.ACCOUNT) {
                ExchangeAttemptBO attempt = createBaseAttempt(authRequest, requestContext)
                        .exchangeFrom(fromTokenType)
                        .exchangeTo(toTokenType)
                        .successful(false)
                        .entityId(sae.getEntityId())
                        .build();

                exchangeAttemptsService.create(attempt);
            }

            emb.publish(CHANNEL, Messages.auth(authMessage, authRequest.getDomain()));
        } else {
            AuthMessage authMessage = AuthMessage.failure(fromTokenType, toTokenType, e);

            emb.publish(CHANNEL, Messages.auth(authMessage, authRequest.getDomain()));
        }
    }

    private ExchangeAttemptBO.Builder createBaseAttempt(final AuthRequestBO authRequest,
                                                        final RequestContextBO requestContext) {
        return ExchangeAttemptBO.builder()
                .clientId(requestContext.getClientId())
                .sourceIp(Optional.ofNullable(authRequest.getSourceIp())
                        .orElse(requestContext.getSource()))
                .deviceId(authRequest.getDeviceId())
                .externalSessionId(authRequest.getExternalSessionId())
                .userAgent(authRequest.getUserAgent());
    }

    private Map<String, Exchange> mapExchanges(final List<Exchange> exchanges) {
        return exchanges.stream()
                .filter(exchange -> exchange.getClass().getAnnotation(TokenExchange.class) != null)
                .collect(Collectors.toMap(
                        this::tokenExchangeToString,
                        Function.identity()
                ));
    }

    private Map<String, AuthProvider> mapProviders(final List<AuthProvider> providers) {
        return providers.stream()
                .filter(provider -> provider.getClass().getAnnotation(ProvidesToken.class) != null)
                .collect(Collectors.toMap(
                        this::authProviderTokenType,
                        Function.identity()
                ));
    }

    private String tokenExchangeToString(final Exchange exchange) {
        TokenExchange tokenExchange = exchange.getClass().getAnnotation(TokenExchange.class);
        return exchangeKey(tokenExchange.from(), tokenExchange.to());
    }

    private String exchangeKey(final String fromTokenType, final String toTokenType) {
        return fromTokenType + "-" + toTokenType;
    }

    private String authProviderTokenType(final AuthProvider authProvider) {
        return authProvider.getClass().getAnnotation(ProvidesToken.class).value();
    }
}
