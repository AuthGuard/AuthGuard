package com.authguard.rest.injectors;


import com.authguard.injection.ClassSearch;
import com.authguard.injection.InjectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

class DynamicBinder {
    private final Logger log = LoggerFactory.getLogger(DynamicBinder.class);

    private final ClassSearch classSearch;

    DynamicBinder(final ClassSearch classSearch) {
        this.classSearch = classSearch;
    }

    <T> Class<? extends T> findBindingsFor(final Class<T> clazz) {
        try {
            final Class<? extends T> impl = classSearch.findImplementationClass(clazz);

            log.debug("Found a binding for {} -> {}", clazz, impl);

            return impl;
        } catch (final InjectionException e) {
            log.error("An exception was thrown during dynamic class search", e);
            System.exit(-1);
            return null;
        }
    }

    <T> Set<Class<? extends T>> findAllBindingsFor(final Class<T> clazz) {
        final Set<Class<? extends T>> impl = classSearch.findAllImplementationClass(clazz);

        log.debug("Found bindings for {} -> {}", clazz, impl);

        return impl;
    }
}
