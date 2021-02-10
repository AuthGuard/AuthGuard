package com.nexblocks.authguard.bindings;

import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.injection.ClassSearch;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExchangesBinder extends AbstractModule {
    private final ConfigContext configContext;
    private final DynamicBinder dynamicBinder;

    public ExchangesBinder(final ConfigContext configContext, final Collection<String> searchPackages) {
        this.configContext = configContext;
        this.dynamicBinder = new DynamicBinder(new ClassSearch(searchPackages));
    }

    @Override
    protected void configure() {
        final Map<String, Class<? extends Exchange>> available = dynamicBinder.findAllBindingsFor(Exchange.class)
                .stream()
                .filter(this::hasTokenExchangeAnnotation)
                .collect(Collectors.toMap(clazz -> exchangeKey(clazz.getAnnotation(TokenExchange.class)), Function.identity()));

        final Collection<ExchangeConfig> allowed = getAllowedExchanges(configContext);

        final Multibinder<Exchange> exchangeMultibinder = Multibinder.newSetBinder(binder(), Exchange.class);

        for (final ExchangeConfig exchangeConfig : allowed) {
            final String key = exchangeKey(exchangeConfig);
            final Class<? extends Exchange> exchange = available.get(key);

            if (exchange == null) {
                throw new UnsupportedOperationException("Token exchange " + exchangeConfig.from + " to "
                        + exchangeConfig.to + " is not supported");
            }

            exchangeMultibinder.addBinding().to(available.get(key));
        }
    }

    @Provides
    List<Exchange> exchangeList(final Set<Exchange> exchangeSet) {
        return new ArrayList<>(exchangeSet);
    }

    private boolean hasTokenExchangeAnnotation(final Class<? extends Exchange> exchangeClass) {
        return exchangeClass.getAnnotation(TokenExchange.class) != null;
    }

    private String exchangeKey(final TokenExchange tokenExchange) {
        return exchangeKey(tokenExchange.from(), tokenExchange.to());
    }

    private String exchangeKey(final ExchangeConfig exchangeConfig) {
        return exchangeKey(exchangeConfig.from, exchangeConfig.to);
    }

    private String exchangeKey(final String from, final String to) {
        return from + "->" + to;
    }

    private Collection<ExchangeConfig> getAllowedExchanges(final ConfigContext configContext) {
        return Optional.ofNullable(configContext.getSubContext("exchange"))
                .map(exchangeContext -> exchangeContext.getAsCollection("allowed", ExchangeConfig.class))
                .orElseGet(Collections::emptyList);
    }

    public static class ExchangeConfig {
        private String from;
        private String to;

        public String getFrom() {
            return from;
        }

        public void setFrom(final String from) {
            this.from = from;
        }

        public String getTo() {
            return to;
        }

        public void setTo(final String to) {
            this.to = to;
        }
    }
}
