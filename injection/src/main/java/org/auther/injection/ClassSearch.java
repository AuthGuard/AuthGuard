package org.auther.injection;

import com.google.inject.AbstractModule;
import org.auther.dal.AccountsRepository;
import org.auther.dal.PermissionsRepository;
import org.reflections.Reflections;

import java.util.HashSet;
import java.util.Set;

public class ClassSearch {
    private final Reflections reflections;
    private Set<Class<? extends AbstractModule>> injectorModules;

    public ClassSearch(final Reflections reflections) {
        this.reflections = reflections;
    }

    public Implementaion<AccountsRepository> findAccountsRepositoryImplementation() throws InjectionException {
        final Class<? extends AccountsRepository> implementationClass = findImplementationClass(AccountsRepository.class);
        final Class<? extends AbstractModule> injectorModule = getInjectorModules().stream()
                .filter(clazz -> clazz.getAnnotation(InjectorModule.class).target().equals(AccountsRepository.class))
                .findFirst()
                .orElse(null);

        return new Implementaion<>(implementationClass, injectorModule);
    }

    public Implementaion<PermissionsRepository> findPermissionsRepositoryImplementation() throws InjectionException {
        final Class<? extends PermissionsRepository> implementationClass = findImplementationClass(PermissionsRepository.class);
        final Class<? extends AbstractModule> injectorModule = getInjectorModules().stream()
                .filter(clazz -> clazz.getAnnotation(InjectorModule.class).target().equals(PermissionsRepository.class))
                .findFirst()
                .orElse(null);

        return new Implementaion<>(implementationClass, injectorModule);
    }

    private Set<Class<? extends AbstractModule>> getInjectorModules() throws InvalidInjectorModule {
        if (injectorModules == null) {
            final Set<Class<?>> modules = reflections.getTypesAnnotatedWith(InjectorModule.class);
            final Set<Class<? extends AbstractModule>> validModules = new HashSet<>(modules.size());

            for (final Class<?> module : modules) {
                if (AbstractModule.class.isAssignableFrom(module)) {
                    validModules.add((Class<? extends AbstractModule>) module);
                } else {
                    throw new InvalidInjectorModule(
                            String.format("Class %s is annotated with %s but does not extend %s", module.getName(),
                                    InjectorModule.class.getSimpleName(), AbstractModule.class.getSimpleName())
                    );
                }
            }

            injectorModules = validModules;
        }

        return injectorModules;
    }

    private <T> Class<? extends T> findImplementationClass(final Class<T> base) throws NoImplementationFoundException {
        final Set<Class<? extends T>> implementations = reflections.getSubTypesOf(base);

        return implementations.stream()
                .filter(clazz -> !clazz.isInterface())
                .findFirst()
                .orElseThrow(() -> new NoImplementationFoundException("No class implementation was found for " + base.getSimpleName()));
    }
}
