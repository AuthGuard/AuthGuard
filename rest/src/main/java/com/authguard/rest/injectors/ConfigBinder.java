package com.authguard.rest.injectors;

import com.authguard.config.ConfigContext;
import com.google.inject.AbstractModule;
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

        bind(ConfigContext.class)
                .toInstance(configContext);
    }
}
