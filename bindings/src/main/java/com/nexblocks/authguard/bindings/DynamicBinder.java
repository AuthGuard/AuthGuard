package com.nexblocks.authguard.bindings;


import com.nexblocks.authguard.injection.ClassSearch;
import com.nexblocks.authguard.injection.InjectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class DynamicBinder {
    private final Logger log = LoggerFactory.getLogger(DynamicBinder.class);

    private final ClassSearch classSearch;

    public DynamicBinder(final ClassSearch classSearch) {
        this.classSearch = classSearch;
    }

    public <T> Class<? extends T> findBindingsFor(final Class<T> clazz) {
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

    public <T> Set<Class<? extends T>> findAllBindingsFor(final Class<T> clazz) {
        final Set<Class<? extends T>> impl = classSearch.findAllImplementationClass(clazz);

        log.debug("Found bindings for {} -> {}", clazz, impl);

        return impl;
    }
}
