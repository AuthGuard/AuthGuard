package com.authguard.rest;

import com.authguard.config.ConfigContext;
import com.authguard.config.JacksonConfigContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

class ConfigurationLoader {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String ROOT_CONFIG_PROPERTY = "authguard";

    private final List<String> expectedFileNames = Arrays.asList(
            "application.json",
            "application.yaml",
            "application.yml"
    );

    public ConfigContext loadFromFile(final String filePath) {
        final File configFile = new File(filePath);

        return new JacksonConfigContext(configFile, getMapper(configFile))
                .getSubContext(ConfigContext.ROOT_CONFIG_PROPERTY);
    }

    public ConfigContext loadFromResources() {
        final File configFile = findFirstConfiguration();

        return new JacksonConfigContext(configFile, getMapper(configFile))
                .getSubContext(ROOT_CONFIG_PROPERTY);
    }

    private File findFirstConfiguration() {
        final ClassLoader classLoader = this.getClass().getClassLoader();

        final URL firstConfig = expectedFileNames.stream()
                .map(classLoader::getResource)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Couldn't find a configuration file. Expecting one of: "
                        + expectedFileNames + " to exist"));

        log.debug("Found configuration " + firstConfig.getFile());

        return new File(firstConfig.getFile());
    }

    private ObjectMapper getMapper(final File configFile) {
        if (configFile.getName().endsWith(".yml") || configFile.getName().endsWith(".yaml")) {
            return yamlMapper();
        } else {
            return jsonMapper();
        }
    }

    private ObjectMapper yamlMapper() {
        return configureMapper(new ObjectMapper(new YAMLFactory()));
    }

    private ObjectMapper jsonMapper() {
        return configureMapper(new ObjectMapper());
    }

    private ObjectMapper configureMapper(final ObjectMapper objectMapper) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }
}
