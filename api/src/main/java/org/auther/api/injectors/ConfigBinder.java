package org.auther.api.injectors;

import com.auther.config.ConfigContext;
import com.auther.config.LightbendConfigContext;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class ConfigBinder extends AbstractModule {
    private final ConfigContext configContext;

    public ConfigBinder() {
        this.configContext = new LightbendConfigContext()
                .getSubContext(ConfigContext.ROOT_CONFIG_PROPERTY);
    }

    @Override
    protected void configure() {
        bind(ConfigContext.class)
                .annotatedWith(Names.named("jwt"))
                .toInstance(configContext.getSubContext("jwt"));

        bind(ConfigContext.class)
                .annotatedWith(Names.named("repository"))
                .toInstance(configContext.getSubContext("repository"));

        bind(ConfigContext.class)
                .toInstance(configContext);
    }
}
