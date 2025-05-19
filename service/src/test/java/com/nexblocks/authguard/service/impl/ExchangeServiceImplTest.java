package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.ExchangeAttemptsService;
import com.nexblocks.authguard.service.ExchangeService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeServiceImplTest {

    @TokenExchange(from = "basic", to = "basic")
    static class ValidExchange implements Exchange {
        @Override
        public CompletableFuture<AuthResponseBO> exchange(final AuthRequestBO request) {
            return CompletableFuture.completedFuture(AuthResponseBO.builder()
                    .token(request.getToken())
                    .type("Basic")
                    .entityType(EntityType.ACCOUNT)
                    .entityId(101)
                    .build());
        }
    }

    static class InvalidExchange implements Exchange {
        @Override
        public CompletableFuture<AuthResponseBO> exchange(final AuthRequestBO request) {
            return CompletableFuture.completedFuture(null);
        }
    }

    @TokenExchange(from = "basic", to = "empty")
    static class EmptyExchange implements Exchange {
        @Override
        public CompletableFuture<AuthResponseBO> exchange(final AuthRequestBO request) {
            return CompletableFuture.failedFuture(new ServiceException(ErrorCode.GENERIC_AUTH_FAILURE, "Empty"));
        }
    }

    @TokenExchange(from = "basic", to = "exception")
    static class ExceptionExchange implements Exchange {
        @Override
        public CompletableFuture<AuthResponseBO> exchange(final AuthRequestBO request) {
            return CompletableFuture.failedFuture(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE, "Empty",
                    EntityType.ACCOUNT, 101));
        }
    }

    @Test
    void exchange() {
        final MessageBus emb = Mockito.mock(MessageBus.class);
        final ExchangeAttemptsService exchangeAttemptsService = Mockito.mock(ExchangeAttemptsService.class);

        final ExchangeService exchangeService = new ExchangeServiceImpl(
                 Arrays.asList(
                        new ValidExchange(),
                        new InvalidExchange(),
                        new ExceptionExchange()),
                Collections.emptyList(),
                exchangeAttemptsService, emb);

        final String basic = "Basic the-rest";
        final AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(basic)
                .deviceId("user-device")
                .externalSessionId("external-session")
                .build();
        final RequestContextBO requestContext = RequestContextBO.builder()
                .clientId("client")
                .source("10.0.0.2")
                .build();

        final AuthResponseBO expected = AuthResponseBO.builder()
                .type("Basic")
                .token(basic)
                .entityType(EntityType.ACCOUNT)
                .entityId(101)
                .build();

        Assertions.assertThat(exchangeService.exchange(authRequest, "basic", "basic", requestContext).join())
                .isEqualTo(expected);

        Mockito.verify(exchangeAttemptsService).create(ExchangeAttemptBO.builder()
                .successful(true)
                .exchangeFrom("basic")
                .exchangeTo("basic")
                .entityId(101L)
                .clientId("client")
                .sourceIp("10.0.0.2")
                .deviceId("user-device")
                .externalSessionId("external-session")
                .build());

        Mockito.verify(emb).publish(Mockito.eq(ExchangeServiceImpl.CHANNEL), Mockito.any());
    }

    @Test
    void exchangeUnknownTokenTypes() {
        final MessageBus emb = Mockito.mock(MessageBus.class);
        final ExchangeAttemptsService exchangeAttemptsService = Mockito.mock(ExchangeAttemptsService.class);

        final ExchangeService exchangeService = new ExchangeServiceImpl(
                Arrays.asList(
                        new ValidExchange(),
                        new InvalidExchange(),
                        new ExceptionExchange()),
                Collections.emptyList(),
                exchangeAttemptsService, emb);

        final String basic = "Basic the-rest";
        final AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(basic)
                .build();
        final RequestContextBO requestContext = RequestContextBO.builder().build();

        assertThatThrownBy(() -> exchangeService.exchange(authRequest, "unknown", "unknown", requestContext).join())
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void exchangeNoTokensGenerated() {
        final MessageBus emb = Mockito.mock(MessageBus.class);
        final ExchangeAttemptsService exchangeAttemptsService = Mockito.mock(ExchangeAttemptsService.class);

        final ExchangeService exchangeService = new ExchangeServiceImpl(
                Collections.singletonList(new EmptyExchange()),
                Collections.emptyList(),
                exchangeAttemptsService, emb);

        final String basic = "Basic the-rest";
        final AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(basic)
                .build();
        final RequestContextBO requestContext = RequestContextBO.builder().build();

        assertThatThrownBy(() -> exchangeService.exchange(authRequest, "basic", "empty", requestContext).join())
                .hasCauseInstanceOf(ServiceException.class);

        Mockito.verify(emb).publish(Mockito.eq(ExchangeServiceImpl.CHANNEL), Mockito.any());
    }

    @Test
    void exchangeServiceAuthorizationException() {
        final MessageBus emb = Mockito.mock(MessageBus.class);
        final ExchangeAttemptsService exchangeAttemptsService = Mockito.mock(ExchangeAttemptsService.class);

        final ExchangeService exchangeService = new ExchangeServiceImpl(
                Collections.singletonList(new ExceptionExchange()),
                Collections.emptyList(),
                exchangeAttemptsService, emb);

        final String basic = "Basic the-rest";
        final AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(basic)
                .build();
        final RequestContextBO requestContext = RequestContextBO.builder().build();

        assertThatThrownBy(() -> exchangeService.exchange(authRequest, "basic", "exception", requestContext).join())
                .hasCauseInstanceOf(ServiceAuthorizationException.class);

        Mockito.verify(exchangeAttemptsService).create(ExchangeAttemptBO.builder()
                .successful(false)
                .exchangeFrom("basic")
                .exchangeTo("exception")
                .entityId(101L)
                .build());

        Mockito.verify(emb).publish(Mockito.eq(ExchangeServiceImpl.CHANNEL), Mockito.any());
    }

    @Test
    void exchangeWithOverriddenProperties() {
        final MessageBus emb = Mockito.mock(MessageBus.class);
        final ExchangeAttemptsService exchangeAttemptsService = Mockito.mock(ExchangeAttemptsService.class);

        final ExchangeService exchangeService = new ExchangeServiceImpl(
                Arrays.asList(
                        new ValidExchange(),
                        new InvalidExchange(),
                        new ExceptionExchange()),
                Collections.emptyList(),
                exchangeAttemptsService, emb);

        final String basic = "Basic the-rest";
        final AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(basic)
                .deviceId("user-device")
                .externalSessionId("external-session")
                .userAgent("user-agent")
                .sourceIp("10.0.0.3")
                .build();
        final RequestContextBO requestContext = RequestContextBO.builder()
                .clientId("client")
                .source("10.0.0.2")
                .build();

        final AuthResponseBO expected = AuthResponseBO.builder()
                .type("Basic")
                .token(basic)
                .entityType(EntityType.ACCOUNT)
                .entityId(101)
                .build();

        Assertions.assertThat(exchangeService.exchange(authRequest, "basic", "basic", requestContext).join())
                .isEqualTo(expected);

        Mockito.verify(exchangeAttemptsService).create(ExchangeAttemptBO.builder()
                .successful(true)
                .exchangeFrom("basic")
                .exchangeTo("basic")
                .entityId(101L)
                .clientId("client")
                .sourceIp("10.0.0.3")
                .deviceId("user-device")
                .externalSessionId("external-session")
                .userAgent("user-agent")
                .build());

        Mockito.verify(emb).publish(Mockito.eq(ExchangeServiceImpl.CHANNEL), Mockito.any());
    }
}