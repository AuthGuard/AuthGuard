package com.authguard.config;

import java.util.Collection;
import java.util.Properties;

public interface ConfigContext {
    String ROOT_CONFIG_PROPERTY = "authguard";

    Object get(String key);
    String getAsString(String key);
    Boolean getAsBoolean(String key);
    <T> Collection<T> getAsCollection(String key, Class<T> targetClass);

    ConfigContext getSubContext(String key);

    <T> T getAsConfigBean(String key, Class<T> clazz);

    <T> T asConfigBean(Class<T> clazz);

    Properties asProperties();

    Iterable<String> subContexts();
}
