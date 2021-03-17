package com.nexblocks.authguard.bindings;

import com.google.inject.AbstractModule;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.injection.ClassSearch;
import com.nexblocks.authguard.service.exchange.ApiKeyExchange;
import com.nexblocks.authguard.service.exchange.KeyExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

public class ApiKeysExchangeBinder extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(ApiKeysExchangeBinder.class);

    private final ConfigContext configContext;
    private final DynamicBinder dynamicBinder;

    public ApiKeysExchangeBinder(final ConfigContext configContext, final Collection<String> searchPackages) {
        this.configContext = configContext;
        this.dynamicBinder = new DynamicBinder(new ClassSearch(searchPackages));
    }

    @Override
    protected void configure() {
        final Optional<String> targetApiKey = getTargetApiKeyType();

        if (targetApiKey.isEmpty()) {
            throw new IllegalStateException("No target API key type was specified in apiKeys.type. Nothing will be bound.");
        }

        final Class<? extends ApiKeyExchange> exchangeClass = dynamicBinder.findAllBindingsFor(ApiKeyExchange.class)
                .stream()
                .filter(this::hasKeyExchangeAnnotation)
                .filter(clazz -> getApiKeyType(clazz).equals(targetApiKey.get()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No exchange was found for API key type " + targetApiKey.get()));

        LOG.info("Will bind API key exchange {}", exchangeClass);

        PluginsRegistry.register(exchangeClass);

        bind(ApiKeyExchange.class).to(exchangeClass);
    }

    private boolean hasKeyExchangeAnnotation(final Class<? extends ApiKeyExchange> exchangeClass) {
        return exchangeClass.getAnnotation(KeyExchange.class) != null;
    }

    private String getApiKeyType(final Class<? extends ApiKeyExchange> exchangeClass) {
        return exchangeClass.getAnnotation(KeyExchange.class).keyType();
    }

    private Optional<String> getTargetApiKeyType() {
        return Optional.of(configContext)
                .filter(context -> context.get("apiKeys") != null)
                .map(context -> context.getSubContext("apiKeys"))
                .filter(context -> context.get("type") != null)
                .map(exchangeContext -> exchangeContext.getAsString("type"));
    }
}
