package org.auther.api.injectors;

import com.auther.config.ConfigContext;
import com.auther.config.LightbendConfigContext;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.auther.service.config.ModifiableJwtConfig;

public class ConfigBinder extends AbstractModule {
    private final ConfigContext configContext;

    public ConfigBinder() {
        this.configContext = new LightbendConfigContext()
                .getSubContext(ConfigContext.ROOT_CONFIG_PROPERTY);
    }

    @Override
    protected void configure() {
        Object jwtConfig = configContext.getAsConfigBean("jwt", ModifiableJwtConfig.class);

        bind(ConfigContext.class)
                .annotatedWith(Names.named("jwt"))
                .toInstance(configContext.getSubContext("jwt"));

        bind(ConfigContext.class)
                .toInstance(configContext);
    }
}
