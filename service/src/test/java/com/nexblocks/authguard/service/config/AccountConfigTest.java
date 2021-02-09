package com.nexblocks.authguard.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountConfigTest {
    @Test
    void parse() {
        final ObjectNode node = new ObjectNode(JsonNodeFactory.instance)
                .put("authguardAdminRole", "admin")
                .put("requireEmail", true)
                .put("requirePhoneNumber", true)
                .put("verifyEmail", true)
                .put("verifyPhoneNumber", true);

        final AccountConfig expected = AccountConfig.builder()
                .authguardAdminRole("admin")
                .requireEmail(true)
                .requirePhoneNumber(true)
                .verifyEmail(true)
                .verifyPhoneNumber(true)
                .build();

        final ObjectMapper mapper = new ObjectMapper();

        assertThat(mapper.convertValue(node, AccountConfig.class)).isEqualTo(expected);
    }
}