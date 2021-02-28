package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.ExchangeAttemptsService;
import com.nexblocks.authguard.service.ExchangeService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.ExchangeAttemptBO;
import com.nexblocks.authguard.service.model.TokensBO;
import io.vavr.control.Either;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeServiceImplTest {

    @TokenExchange(from = "basic", to = "basic")
    static class ValidExchange implements Exchange {
        @Override
        public Either<Exception, TokensBO> exchange(final AuthRequestBO request) {
            return Either.right(TokensBO.builder()
                    .token(request.getToken())
                    .type("Basic")
                    .entityType(EntityType.ACCOUNT)
                    .entityId("account")
                    .build());
        }
    }

    static class InvalidExchange implements Exchange {
        @Override
        public Either<Exception, TokensBO> exchange(final AuthRequestBO request) {
            return Either.right(null);
        }
    }

    @TokenExchange(from = "basic", to = "empty")
    static class EmptyExchange implements Exchange {
        @Override
        public Either<Exception, TokensBO> exchange(final AuthRequestBO request) {
            return Either.left(new ServiceException(ErrorCode.GENERIC_AUTH_FAILURE, "Empty"));
        }
    }

    @TokenExchange(from = "basic", to = "exception")
    static class ExceptionExchange implements Exchange {
        @Override
        public Either<Exception, TokensBO> exchange(final AuthRequestBO request) {
            return Either.left(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE, "Empty",
                    EntityType.ACCOUNT, "account"));
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
                .build();

        final TokensBO expected = TokensBO.builder()
                .type("Basic")
                .token(basic)
                .entityType(EntityType.ACCOUNT)
                .entityId("account")
                .build();

        Assertions.assertThat(exchangeService.exchange(authRequest, "basic", "basic")).isEqualTo(expected);

        Mockito.verify(exchangeAttemptsService).create(ExchangeAttemptBO.builder()
                .successful(true)
                .exchangeFrom("basic")
                .exchangeTo("basic")
                .entityId("account")
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

        assertThatThrownBy(() -> exchangeService.exchange(authRequest, "unknown", "unknown"))
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

        assertThatThrownBy(() -> exchangeService.exchange(authRequest, "basic", "empty"))
                .isInstanceOf(ServiceException.class);

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

        assertThatThrownBy(() -> exchangeService.exchange(authRequest, "basic", "exception"))
                .isInstanceOf(ServiceAuthorizationException.class);

        Mockito.verify(exchangeAttemptsService).create(ExchangeAttemptBO.builder()
                .successful(false)
                .exchangeFrom("basic")
                .exchangeTo("exception")
                .entityId("account")
                .build());

        Mockito.verify(emb).publish(Mockito.eq(ExchangeServiceImpl.CHANNEL), Mockito.any());
    }
}