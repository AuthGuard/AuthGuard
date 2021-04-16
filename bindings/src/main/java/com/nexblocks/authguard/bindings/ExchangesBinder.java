package com.nexblocks.authguard.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.injection.ClassSearch;
import com.nexblocks.authguard.service.auth.AuthProvider;
import com.nexblocks.authguard.service.auth.ProvidesToken;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExchangesBinder extends AbstractModule {
    private Logger LOG = LoggerFactory.getLogger(ExchangesBinder.class);

    private final ConfigContext configContext;
    private final DynamicBinder dynamicBinder;

    public ExchangesBinder(final ConfigContext configContext, final Collection<String> searchPackages) {
        this.configContext = configContext;
        this.dynamicBinder = new DynamicBinder(new ClassSearch(searchPackages));
    }

    @Override
    protected void configure() {
        configureExchanges();
        configureProviders();
    }

    @Provides
    List<Exchange> exchangeList(final Set<Exchange> exchangeSet) {
        return new ArrayList<>(exchangeSet);
    }

    @Provides
    List<AuthProvider> providersList(final Set<AuthProvider> authProviderSet) {
        return new ArrayList<>(authProviderSet);
    }

    private void configureExchanges() {
        final Map<String, Class<? extends Exchange>> available = dynamicBinder.findAllBindingsFor(Exchange.class)
                .stream()
                .peek(clazz -> LOG.debug("Found exchange {}", clazz.getCanonicalName()))
                .filter(clazz -> {
                    if (!hasTokenExchangeAnnotation(clazz)) {
                        LOG.warn("Exchange {} will be ignored since it is not annotated with TokenExchange", clazz.getCanonicalName());
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toMap(clazz -> exchangeKey(clazz.getAnnotation(TokenExchange.class)), Function.identity()));

        final Collection<ExchangeConfig> allowed = getAllowedExchanges(configContext);

        final Multibinder<Exchange> exchangeMultibinder = Multibinder.newSetBinder(binder(), Exchange.class);

        for (final ExchangeConfig exchangeConfig : allowed) {
            final String key = exchangeKey(exchangeConfig);
            final Class<? extends Exchange> exchange = available.get(key);

            if (exchange == null) {
                throw new ConfigurationException("Token exchange " + exchangeConfig.from + " to "
                        + exchangeConfig.to + " is not available");
            }

            PluginsRegistry.register(exchange);

            exchangeMultibinder.addBinding().to(available.get(key));
        }
    }

    private void configureProviders() {
        final Collection<String> allowed = getAllowedProviders(configContext);
        final Multibinder<AuthProvider> authProvidersMultibinder = Multibinder.newSetBinder(binder(), AuthProvider.class);

        dynamicBinder.findAllBindingsFor(AuthProvider.class)
                .forEach(clazz -> {
                    final Optional<String> tokenType = providedTokenType(clazz);

                    if (tokenType.isPresent()) {
                        if (allowed.contains(tokenType.get())) {
                            LOG.debug("Found auth provider {}", clazz.getCanonicalName());

                            PluginsRegistry.register(clazz);
                            authProvidersMultibinder.addBinding().to(clazz);
                        } else {
                            LOG.debug("Skipping auth provider {} since its type ({}) is not allowed",
                                    clazz.getCanonicalName(), tokenType.get());
                        }
                    } else {
                        LOG.warn("Auth provider {} will be ignored since it is not annotated with ProvidesToken",
                                clazz.getCanonicalName());
                    }
                });
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

    private Optional<String> providedTokenType(final Class<? extends AuthProvider> providerClass) {
        return Optional.ofNullable(providerClass.getAnnotation(ProvidesToken.class))
                .map(ProvidesToken::value);
    }

    private Collection<ExchangeConfig> getAllowedExchanges(final ConfigContext configContext) {
        return Optional.ofNullable(configContext.getSubContext("exchange"))
                .map(exchangeContext -> exchangeContext.getAsCollection("allowed", ExchangeConfig.class))
                .orElseGet(Collections::emptyList);
    }

    private Collection<String> getAllowedProviders(final ConfigContext configContext) {
        return Optional.ofNullable(configContext.getSubContext("exchange"))
                .map(exchangeContext -> exchangeContext.getAsCollection("providers", String.class))
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
