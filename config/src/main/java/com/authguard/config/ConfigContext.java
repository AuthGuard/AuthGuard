package com.authguard.config;

import java.util.Collection;

public interface ConfigContext {
    String ROOT_CONFIG_PROPERTY = "authguard";

    Object get(String key);
    String getAsString(String key);
    boolean getAsBoolean(String key);
    <T> Collection<T> getAsCollection(String key, Class<T> targetClass);

    ConfigContext getSubContext(String key);

    <T> T getAsConfigBean(String key, Class<T> clazz);

    <T> T asConfigBean(Class<T> clazz);

    Iterable<String> subContexts();
}
