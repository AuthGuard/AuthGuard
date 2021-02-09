package com.nexblocks.authguard.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StrategyConfigTest {
    @Test
    void parse() {
        final ObjectNode node = new ObjectNode(JsonNodeFactory.instance)
                .put("tokenLife", "5m")
                .put("refreshTokenLife", "5m")
                .put("useJti", true)
                .put("includePermissions", true)
                .put("includeRoles", true)
                .put("includeExternalId", true);

        final StrategyConfig expected = StrategyConfig.builder()
                .tokenLife("5m")
                .refreshTokenLife("5m")
                .useJti(true)
                .includeExternalId(true)
                .includePermissions(true)
                .includeRoles(true)
                .build();

        final ObjectMapper mapper = new ObjectMapper();

        assertThat(mapper.convertValue(node, StrategyConfig.class)).isEqualTo(expected);
    }
}