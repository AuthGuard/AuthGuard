package org.auther.rest.injectors;

import com.auther.config.ConfigContext;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class ConfigBinder extends AbstractModule {
    private final ConfigContext configContext;

    public ConfigBinder(final ConfigContext configContext) {
        this.configContext = configContext;
    }

    @Override
    protected void configure() {
        bind(ConfigContext.class)
                .annotatedWith(Names.named("jwt"))
                .toInstance(configContext.getSubContext("jwt"));

        bind(ConfigContext.class)
                .toInstance(configContext);
    }
}
