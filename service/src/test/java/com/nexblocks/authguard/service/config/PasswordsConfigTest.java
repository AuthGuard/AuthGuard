package com.nexblocks.authguard.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordsConfigTest {
    @Test
    void parseWithDefaultAlgorithmConfig() {
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

    @Test
    void parseSCryptConfig() {
        final ObjectNode scrypt = new ObjectNode(JsonNodeFactory.instance)
                .put("CPUMemoryCostParameter", 3)
                .put("blockSize", 16)
                .put("parallelization", 2)
                .put("saltSize", 2)
                .put("keySize", 6);

        final ObjectNode node = new ObjectNode(JsonNodeFactory.instance)
                .put("algorithm", "scrypt");

        node.set("scrypt", scrypt);

        final PasswordsConfig expected = PasswordsConfig.builder()
                .algorithm("scrypt")
                .scrypt(SCryptConfig.builder()
                        .cPUMemoryCostParameter(3)
                        .blockSize(16)
                        .parallelization(2)
                        .saltSize(2)
                        .keySize(6)
                        .build())
                .build();

        final ObjectMapper mapper = new ObjectMapper();

        assertThat(mapper.convertValue(node, PasswordsConfig.class)).isEqualTo(expected);
    }

    @Test
    void parseBCryptConfig() {
        final ObjectNode bcrypt = new ObjectNode(JsonNodeFactory.instance)
                .put("cost", 2)
                .put("saltSize", 2);

        final ObjectNode node = new ObjectNode(JsonNodeFactory.instance)
                .put("algorithm", "bcrypt");

        node.set("bcrypt", bcrypt);

        final PasswordsConfig expected = PasswordsConfig.builder()
                .algorithm("bcrypt")
                .bcrypt(BCryptConfig.builder()
                        .cost(2)
                        .saltSize(2)
                        .build())
                .build();

        final ObjectMapper mapper = new ObjectMapper();

        assertThat(mapper.convertValue(node, PasswordsConfig.class)).isEqualTo(expected);
    }
}