package com.authguard.rest.injectors;

import com.authguard.config.ConfigContext;
import com.authguard.config.EmptyConfigContext;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigBinder extends AbstractModule {
    private final ConfigContext configContext;

    public ConfigBinder(final ConfigContext configContext) {
        this.configContext = configContext;
    }

    @Override
    protected void configure() {
        final List<String> optionalContexts = Arrays.asList("otp", "verification");
        final List<String> boundContexts = new ArrayList<>();

        configContext.subContexts().forEach(subContext -> {
            bind(ConfigContext.class)
                .annotatedWith(Names.named(subContext))
                .toInstance(configContext.getSubContext(subContext));

            boundContexts.add(subContext);
        });

        optionalContexts.forEach(context -> {
            if (!boundContexts.contains(context)) {
                bind(ConfigContext.class)
                        .annotatedWith(Names.named(context))
                        .to(EmptyConfigContext.class);
            }
        });

        bind(ConfigContext.class)
                .toInstance(configContext);
    }
}
