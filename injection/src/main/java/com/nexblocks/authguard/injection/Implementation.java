package com.nexblocks.authguard.injection;

import com.google.inject.AbstractModule;

public class Implementation<T> {
    private final Class<? extends T> implementationClass;
    private final Class<? extends AbstractModule> injectorModule;

    public Implementation(final Class<? extends T> implementationClass, final Class<? extends AbstractModule> injectorModule) {
        this.implementationClass = implementationClass;
        this.injectorModule = injectorModule;
    }

    public Class<? extends T> getImplementationClass() {
        return implementationClass;
    }

    public Class<? extends AbstractModule> getInjectorModule() {
        return injectorModule;
    }
}
