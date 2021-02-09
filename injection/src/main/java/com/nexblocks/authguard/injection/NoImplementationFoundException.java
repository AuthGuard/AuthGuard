package com.nexblocks.authguard.injection;

public class NoImplementationFoundException extends InjectionException {
    public NoImplementationFoundException(final String message) {
        super(message);
    }
}
