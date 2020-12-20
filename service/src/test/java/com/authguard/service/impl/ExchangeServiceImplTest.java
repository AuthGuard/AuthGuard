package com.authguard.service.impl;

import com.authguard.dal.ExchangeAttemptsRepository;
import com.authguard.dal.model.ExchangeAttemptDO;
import com.authguard.emb.MessageBus;
import com.authguard.service.ExchangeService;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.EntityType;
import com.authguard.service.model.TokensBO;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
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
        final ExchangeAttemptsRepository exchangeAttemptsRepository = Mockito.mock(ExchangeAttemptsRepository.class);

        final ExchangeService exchangeService = new ExchangeServiceImpl(
                Arrays.asList(
                        new ValidExchange(),
                        new InvalidExchange(),
                        new ExceptionExchange()),
                exchangeAttemptsRepository, emb);

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

        assertThat(exchangeService.exchange(authRequest, "basic", "basic")).isEqualTo(expected);

        Mockito.verify(exchangeAttemptsRepository).save(ExchangeAttemptDO.builder()
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
        final ExchangeAttemptsRepository exchangeAttemptsRepository = Mockito.mock(ExchangeAttemptsRepository.class);

        final ExchangeService exchangeService = new ExchangeServiceImpl(
                Arrays.asList(
                        new ValidExchange(),
                        new InvalidExchange(),
                        new ExceptionExchange()),
                exchangeAttemptsRepository, emb);

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
        final ExchangeAttemptsRepository exchangeAttemptsRepository = Mockito.mock(ExchangeAttemptsRepository.class);

        final ExchangeService exchangeService = new ExchangeServiceImpl(Collections.singletonList(new EmptyExchange()),
                exchangeAttemptsRepository, emb);

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
        final ExchangeAttemptsRepository exchangeAttemptsRepository = Mockito.mock(ExchangeAttemptsRepository.class);

        final ExchangeService exchangeService = new ExchangeServiceImpl(Collections.singletonList(new ExceptionExchange()),
                exchangeAttemptsRepository, emb);

        final String basic = "Basic the-rest";
        final AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(basic)
                .build();

        assertThatThrownBy(() -> exchangeService.exchange(authRequest, "basic", "exception"))
                .isInstanceOf(ServiceAuthorizationException.class);

        Mockito.verify(exchangeAttemptsRepository).save(ExchangeAttemptDO.builder()
                .successful(false)
                .exchangeFrom("basic")
                .exchangeTo("exception")
                .entityId("account")
                .build());

        Mockito.verify(emb).publish(Mockito.eq(ExchangeServiceImpl.CHANNEL), Mockito.any());
    }
}