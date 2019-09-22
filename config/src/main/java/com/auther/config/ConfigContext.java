package com.auther.config;

public interface ConfigContext {
    String ROOT_CONFIG_PROPERTY = "authguard";

    Object get(String key);
    String getAsString(String key);
    boolean getAsBoolean(String key);

    ConfigContext getSubContext(String key);
}
