package com.nexblocks.authguard.config;

public class ValuesResolver {
    static String resolve(final String value) {
        if (value.startsWith("env:")) {
            final String envVariable = value.substring(4);

            if (envVariable.isBlank()) {
                throw new IllegalArgumentException("Empty environment variable name. Value: " + value);
            }

            return System.getenv(envVariable);
        }

        if (value.startsWith("system:")) {
            final String propertyName = value.substring(7);

            if (propertyName.isBlank()) {
                throw new IllegalArgumentException("Empty environment variable name. Value: " + value);
            }

            return System.getProperty(propertyName);
        }

        return value;
    }
}
