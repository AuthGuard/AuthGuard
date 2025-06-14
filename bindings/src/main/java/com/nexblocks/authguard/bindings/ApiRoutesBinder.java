package com.nexblocks.authguard.bindings;

import com.google.inject.AbstractModule;
import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.config.ConfigContext;

import java.util.Collection;
import java.util.Optional;

@Deprecated
public class ApiRoutesBinder extends AbstractModule {

    public ApiRoutesBinder(final Collection<String> searchPackages, final ConfigContext configContext) {
    }

    @Override
    protected void configure() {

    }

    private Optional<String> dependsOnConfiguration(final Class<?> clazz) {
        return Optional.ofNullable(clazz.getAnnotation(DependsOnConfiguration.class))
                .map(DependsOnConfiguration::value);
    }
}
