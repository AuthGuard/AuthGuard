package com.authguard.bindings;

import com.authguard.api.annotations.DependsOnConfiguration;
import com.authguard.api.routes.ApiRoute;
import com.authguard.config.ConfigContext;
import com.authguard.injection.ClassSearch;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ApiRoutesBinder extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(ApiRoutesBinder.class);

    private final DynamicBinder dynamicBinder;
    private final ConfigContext configContext;

    public ApiRoutesBinder(final Collection<String> searchPackages, final ConfigContext configContext) {
        this.dynamicBinder = new DynamicBinder(new ClassSearch(searchPackages));
        this.configContext = configContext;
    }

    @Override
    protected void configure() {
        final Set<Class<? extends ApiRoute>> available = dynamicBinder.findAllBindingsFor(ApiRoute.class);
        final Set<Class<? extends ApiRoute>> eligible = available.stream()
                .filter(routeClass -> dependsOnConfiguration(routeClass)
                        .map(property -> {
                            if (configContext.get(property) == null) {
                                LOG.info("Route {} will be skipped because property '{}' wasn't found in the config",
                                        routeClass.getSimpleName(), property);
                                return false;
                            }

                            return true;
                        })
                        .orElse(true))
                .collect(Collectors.toSet());

        LOG.info("Found {} routes, {} of them were eligible for binding", available.size(), eligible.size());

        final Multibinder<ApiRoute> routesMultibinder = Multibinder.newSetBinder(binder(), ApiRoute.class);

        eligible.forEach(route -> routesMultibinder.addBinding().to(route));
    }

    private Optional<String> dependsOnConfiguration(final Class<?> clazz) {
        return Optional.ofNullable(clazz.getAnnotation(DependsOnConfiguration.class))
                .map(DependsOnConfiguration::value);
    }
}
