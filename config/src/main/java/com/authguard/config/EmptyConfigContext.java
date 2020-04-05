package com.authguard.config;

import java.util.Collection;

public class EmptyConfigContext implements ConfigContext {
    @Override
    public Object get(String key) {
        return null;
    }

    @Override
    public String getAsString(String key) {
        return null;
    }

    @Override
    public boolean getAsBoolean(String key) {
        return false;
    }

    @Override
    public <T> Collection<T> getAsCollection(String key, Class<T> targetClass) {
        return null;
    }

    @Override
    public ConfigContext getSubContext(String key) {
        return null;
    }

    @Override
    public <T> T getAsConfigBean(String key, Class<T> clazz) {
        return null;
    }

    @Override
    public <T> T asConfigBean(Class<T> clazz) {
        return null;
    }

    @Override
    public Iterable<String> subContexts() {
        return null;
    }
}
