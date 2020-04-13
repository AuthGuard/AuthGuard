package com.authguard.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

public class JacksonConfigContext implements ConfigContext {
    private final ObjectNode rootNode;
    private final ObjectMapper objectMapper;

    public JacksonConfigContext(final File configFile) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.rootNode = readRootNode(configFile);
    }

    public JacksonConfigContext(final File configFile, final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.rootNode = readRootNode(configFile);
    }

    public JacksonConfigContext(final JsonNode rootNode) {
        if (!rootNode.isObject()) {
            throw new IllegalArgumentException("Invalid JSON configuration");
        }
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.rootNode = (ObjectNode) rootNode;
    }

    private ObjectNode readRootNode(final File configFile) {
        try {
            return Optional.ofNullable(objectMapper.readTree(configFile))
                    .filter(JsonNode::isObject)
                    .map(node -> (ObjectNode) node)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid configuration"));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Object get(final String key) {
        return rootNode.get(key);
    }

    @Override
    public String getAsString(final String key) {
        return rootNode.get(key).asText();
    }

    @Override
    public boolean getAsBoolean(final String key) {
        return rootNode.get(key).asBoolean();
    }

    @Override
    public <T> Collection<T> getAsCollection(final String key, final Class<T> targetClass) {
        final JsonNode child = rootNode.get(key);

        if (child.isArray()) {
            return objectMapper.convertValue(child,
                    objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, targetClass));
        } else {
            throw new RuntimeException("Field " + key + " does not represent a JSON array");
        }
    }

    @Override
    public ConfigContext getSubContext(final String key) {
        return new JacksonConfigContext(rootNode.get(key));
    }

    @Override
    public <T> T getAsConfigBean(final String key, final Class<T> clazz) {
        try {
            return objectMapper.treeToValue(rootNode.get(key), clazz);
        } catch (final JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <T> T asConfigBean(final Class<T> clazz) {
        try {
            return objectMapper.treeToValue(rootNode, clazz);
        } catch (final JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Iterable<String> subContexts() {
        return rootNode::fieldNames;
    }

    @Override
    public String toString() {
        return rootNode.toString();
    }
}
