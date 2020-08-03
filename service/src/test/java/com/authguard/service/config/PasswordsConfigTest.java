package com.authguard.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordsConfigTest {
    @Test
    void parse() {
        final ObjectNode conditions = new ObjectNode(JsonNodeFactory.instance)
                .put("minLength", 10)
                .put("maxLength", 16)
                .put("includeCaps", true)
                .put("includeDigits", true)
                .put("includeSmallLetters", false)
                .put("includeSpecialCharacters", true);

        final ObjectNode node = new ObjectNode(JsonNodeFactory.instance)
                .put("algorithm", "scrypt");

        node.set("conditions", conditions);

        final PasswordsConfig expected = PasswordsConfig.builder()
                .algorithm("scrypt")
                .conditions(PasswordConditions.builder()
                        .minLength(10)
                        .maxLength(16)
                        .includeCaps(true)
                        .includeDigits(true)
                        .includeSmallLetters(false)
                        .includeSpecialCharacters(true)
                        .build())
                .build();

        final ObjectMapper mapper = new ObjectMapper();

        assertThat(mapper.convertValue(node, PasswordsConfig.class)).isEqualTo(expected);
    }
}