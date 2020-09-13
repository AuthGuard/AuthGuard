package com.authguard.api.dto.validation.requests;

import com.authguard.api.dto.requests.RolesRequest;
import com.authguard.api.dto.requests.RolesRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RolesRequestDTOTest {
    @Test
    void parse() {
        final ArrayNode roles = new ArrayNode(JsonNodeFactory.instance)
                .add("test");

        final ObjectNode requestJson = new ObjectNode(JsonNodeFactory.instance)
                .put("action", "GRANT")
                .set("roles", roles);

        final ObjectMapper objectMapper = new ObjectMapper();

        final RolesRequestDTO expected = RolesRequestDTO.builder()
                .action(RolesRequest.Action.GRANT)
                .addRoles("test")
                .build();

        final RolesRequestDTO actual = objectMapper.convertValue(requestJson, RolesRequestDTO.class);

        assertThat(actual).isEqualTo(expected);
    }
}
