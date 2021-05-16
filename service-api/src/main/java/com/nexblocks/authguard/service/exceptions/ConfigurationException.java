package com.nexblocks.authguard.service.exceptions;

/**
 * An exception to be thrown if a configuration is invalid. It
 * should prevent the process from starting.
 */
public class ConfigurationException extends RuntimeException {
    public ConfigurationException(final String message) {
        super(message);
    }

    public ConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
