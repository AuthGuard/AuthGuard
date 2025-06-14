package com.nexblocks.authguard.bindings;

import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangesBinderTest {
    public static class NeedsExchanges {
        List<Exchange> exchanges;

        @Inject
        public NeedsExchanges(final List<Exchange> exchanges) {
            this.exchanges = exchanges;
        }
    }

    @TokenExchange(from = "basic", to = "random")
    public static class FirstExchange implements Exchange {
        @Override
        public Uni<AuthResponseBO> exchange(final AuthRequestBO request)  {
            return Uni.createFrom().item(null);
        }
    }

    @TokenExchange(from = "random", to = "chaos")
    public static class SecondExchange implements Exchange {
        @Override
        public Uni<AuthResponseBO> exchange(final AuthRequestBO request) {
            return Uni.createFrom().item(null);
        }
    }

    @Test
    void exchangesInjected() {
        final ConfigContext rootContext = Mockito.mock(ConfigContext.class);
        final ConfigContext exchangeContext = Mockito.mock(ConfigContext.class);

        final ExchangesBinder.ExchangeConfig allowedExchange = new ExchangesBinder.ExchangeConfig();

        allowedExchange.setFrom("basic");
        allowedExchange.setTo("random");

        Mockito.when(rootContext.getSubContext("exchange")).thenReturn(exchangeContext);
        Mockito.when(exchangeContext.getAsCollection("allowed", ExchangesBinder.ExchangeConfig.class))
                .thenReturn(Collections.singletonList(allowedExchange));

        final Collection<String> searchPackages = Collections.singletonList("com.nexblocks.authguard.bindings");

        final Injector injector = Guice.createInjector(new ExchangesBinder(rootContext, searchPackages));

        final NeedsExchanges needsExchanges = injector.getInstance(NeedsExchanges.class);

        assertThat(needsExchanges.exchanges).hasSize(1);
        assertThat(needsExchanges.exchanges.get(0)).isInstanceOf(FirstExchange.class);
    }

    @Test
    void unsupportedExchange() {
        final ConfigContext rootContext = Mockito.mock(ConfigContext.class);
        final ConfigContext exchangeContext = Mockito.mock(ConfigContext.class);

        final ExchangesBinder.ExchangeConfig allowedExchange = new ExchangesBinder.ExchangeConfig();

        allowedExchange.setFrom("unsupported");
        allowedExchange.setTo("random");

        Mockito.when(rootContext.getSubContext("exchange")).thenReturn(exchangeContext);
        Mockito.when(exchangeContext.getAsCollection("allowed", ExchangesBinder.ExchangeConfig.class))
                .thenReturn(Collections.singletonList(allowedExchange));

        final Collection<String> searchPackages = Collections.singletonList("com.nexblocks.authguard.bindings");

        assertThatThrownBy(() -> Guice.createInjector(new ExchangesBinder(rootContext, searchPackages)))
                .isInstanceOf(CreationException.class)
                .extracting("cause")
                .isInstanceOf(ConfigurationException.class);
    }
}