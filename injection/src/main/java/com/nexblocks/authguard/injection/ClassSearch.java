package com.nexblocks.authguard.injection;

import com.google.inject.AbstractModule;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassSearch {
    private final Reflections reflections;
    private Set<Class<? extends AbstractModule>> injectorModules;

    public ClassSearch(final Collection<String> searchPackages) {
        this.reflections = new Reflections(searchPackages);
    }

    public ClassSearch(final Reflections reflections) {
        this.reflections = reflections;
    }

    public <T> Class<? extends T> findImplementationClass(final Class<T> base) throws NoImplementationFoundException {
        final Set<Class<? extends T>> implementations = reflections.getSubTypesOf(base);

        return implementations.stream()
                .filter(clazz -> !clazz.isInterface())
                .findFirst()
                .orElseThrow(() -> new NoImplementationFoundException("No class implementation was found for " + base.getSimpleName()));
    }

    public <T> Set<Class<? extends T>> findAllImplementationClass(final Class<T> base) {
        final Set<Class<? extends T>> implementations = reflections.getSubTypesOf(base);

        return implementations.stream()
                .filter(clazz -> !clazz.isInterface())
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .collect(Collectors.toSet());
    }
}
