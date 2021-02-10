package com.nexblocks.authguard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

public class EmptyConfigContext implements ConfigContext {
    private final ObjectNode emptyNode;
    private final ObjectMapper objectMapper;

    public EmptyConfigContext() {
        objectMapper = new ObjectMapper();
        emptyNode = objectMapper.createObjectNode();
    }

    @Override
    public Object get(String key) {
        return null;
    }

    @Override
    public String getAsString(String key) {
        return null;
    }

    @Override
    public Boolean getAsBoolean(String key) {
        return null;
    }

    @Override
    public <T> Collection<T> getAsCollection(String key, Class<T> targetClass) {
        return null;
    }

    @Override
    public ConfigContext getSubContext(String key) {
        return new EmptyConfigContext();
    }

    @Override
    public <T> T getAsConfigBean(String key, Class<T> clazz) {
        return null;
    }

    @Override
    public <T> T asConfigBean(Class<T> clazz) {
        return objectMapper.convertValue(emptyNode, clazz);
    }

    @Override
    public Properties asProperties() {
        return new Properties();
    }

    @Override
    public Iterable<String> subContexts() {
        return Collections.emptyList();
    }
}
