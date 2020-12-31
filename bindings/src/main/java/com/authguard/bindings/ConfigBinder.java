package com.authguard.bindings;

import com.authguard.config.ConfigContext;
import com.authguard.config.EmptyConfigContext;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class ConfigBinder extends AbstractModule {
    private final ConfigContext configContext;

    public ConfigBinder(final ConfigContext configContext) {
        this.configContext = configContext;
    }

    @Override
    protected void configure() {
        configContext.subContexts().forEach(subContext -> {
            bind(ConfigContext.class)
                .annotatedWith(Names.named(subContext))
                .toInstance(configContext.getSubContext(subContext));
        });

        // if nothing was
        bind(ConfigContext.class)
                .annotatedWith(Named.class)
                .toInstance(new EmptyConfigContext());

        bind(ConfigContext.class)
                .toInstance(configContext);
    }
}
