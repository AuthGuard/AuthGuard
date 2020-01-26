package com.authguard.injection;

public class InvalidInjectorModule extends InjectionException {
    public InvalidInjectorModule(final String message) {
        super(message);
    }
}
