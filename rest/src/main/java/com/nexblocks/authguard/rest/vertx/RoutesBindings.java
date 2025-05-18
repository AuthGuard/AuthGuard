package com.nexblocks.authguard.rest.vertx;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.bindings.ApiRoutesBinder;
import com.nexblocks.authguard.bindings.DynamicBinder;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.injection.ClassSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RoutesBindings extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(ApiRoutesBinder.class);

    private final DynamicBinder dynamicBinder;
    private final ConfigContext configContext;

    public RoutesBindings(final Collection<String> searchPackages, final ConfigContext configContext) {
        this.dynamicBinder = new DynamicBinder(new ClassSearch(searchPackages));
        this.configContext = configContext;
    }

    @Override
    protected void configure() {
        Set<Class<? extends VertxApiHandler>> available = dynamicBinder.findAllBindingsFor(VertxApiHandler.class);
        Set<Class<? extends VertxApiHandler>> eligible = available.stream()
                .filter(routeClass -> dependsOnConfiguration(routeClass)
                        .map(property -> {
                            if (configContext.get(property) == null) {
                                LOG.info("Route {} will be skipped because property '{}' wasn't found in the application configuration",
                                        routeClass.getSimpleName(), property);
                                return false;
                            }

                            return true;
                        })
                        .orElse(true))
                .collect(Collectors.toSet());

        LOG.info("Found {} routes, {} of them were eligible for binding", available.size(), eligible.size());

        Multibinder<VertxApiHandler> routesMultibinder = Multibinder.newSetBinder(binder(), VertxApiHandler.class);

        eligible.forEach(route -> {
            try {
                routesMultibinder.addBinding().to(route);
            } catch (Throwable e) {
                LOG.error("Failed to bind route");
            }
        });
    }

    private Optional<String> dependsOnConfiguration(final Class<?> clazz) {
        return Optional.ofNullable(clazz.getAnnotation(DependsOnConfiguration.class))
                .map(DependsOnConfiguration::value);
    }

}
