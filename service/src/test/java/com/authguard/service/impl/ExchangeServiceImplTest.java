package com.authguard.service.impl;

import com.authguard.service.ExchangeService;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.TokensBO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeServiceImplTest {

    @TokenExchange(from = "basic", to = "basic")
    static class ValidExchange implements Exchange {
        @Override
        public Optional<TokensBO> exchangeToken(final String fromToken) {
            return Optional.of(TokensBO.builder()
                    .token(fromToken)
                    .type("Basic")
                    .build());
        }
    }

    static class InvalidExchange implements Exchange {
        @Override
        public Optional<TokensBO> exchangeToken(final String fromToken) {
            return Optional.empty();
        }
    }

    @TokenExchange(from = "basic", to = "empty")
    static class EmptyExchange implements Exchange {
        @Override
        public Optional<TokensBO> exchangeToken(final String fromToken) {
            return Optional.empty();
        }
    }

    @Test
    void exchange() {
        final ExchangeService exchangeService = new ExchangeServiceImpl(
                Arrays.asList(new ValidExchange(), new InvalidExchange()));

        final String basic = "Basic the-rest";
        final TokensBO expected = TokensBO.builder()
                .type("Basic")
                .token(basic)
                .build();

        assertThat(exchangeService.exchange(basic, "basic", "basic")).isEqualTo(expected);
        assertThatThrownBy(() -> exchangeService.exchange(basic, "unknown", "unknown"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void exchangeUnknownTokenTypes() {
        final ExchangeService exchangeService = new ExchangeServiceImpl(
                Arrays.asList(new ValidExchange(), new InvalidExchange()));

        final String basic = "Basic the-rest";

        assertThatThrownBy(() -> exchangeService.exchange(basic, "unknown", "unknown"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void exchangeNoTokensGenerated() {
        final ExchangeService exchangeService = new ExchangeServiceImpl(Collections.singletonList(new EmptyExchange()));

        final String basic = "Basic the-rest";

        assertThatThrownBy(() -> exchangeService.exchange(basic, "basic", "empty"))
                .isInstanceOf(ServiceException.class);
    }
}