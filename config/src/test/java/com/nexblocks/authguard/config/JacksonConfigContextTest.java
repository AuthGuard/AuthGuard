package com.nexblocks.authguard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigContextTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void missingProperties() {
        final ObjectNode rootNode = objectMapper.createObjectNode();

        final JacksonConfigContext configContext = new JacksonConfigContext(rootNode);

        assertThat(configContext.get("missing")).isNull();

        assertThat(configContext.getAsString("missing")).isNull();
        assertThat(configContext.getAsBoolean("missing")).isNull();

        assertThat(configContext.getAsConfigBean("missing", String.class)).isNull();

        assertThat(configContext.getSubContext("missing")).isNull();
    }

    @Test
    void asProperties() {
        final ObjectNode rootNode = objectMapper.createObjectNode();

        rootNode.put("plain", "value");
        rootNode.put("nested.property", "nested_value");

        final JacksonConfigContext configContext = new JacksonConfigContext(rootNode);

        final Properties expected = new Properties();

        expected.put("plain", "value");
        expected.put("nested.property", "nested_value");

        final Properties actual = configContext.asProperties();

        assertThat(actual).isEqualTo(expected);
    }
}