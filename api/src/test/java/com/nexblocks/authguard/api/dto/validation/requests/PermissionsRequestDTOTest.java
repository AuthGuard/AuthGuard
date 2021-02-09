package com.nexblocks.authguard.api.dto.validation.requests;

import com.nexblocks.authguard.api.dto.entities.PermissionDTO;
import com.nexblocks.authguard.api.dto.requests.PermissionsRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionsRequestDTOTest {
    @Test
    void parse() {
        final ArrayNode permissions = new ArrayNode(JsonNodeFactory.instance)
                .add(new ObjectNode(JsonNodeFactory.instance)
                        .put("group", "tests")
                        .put("name", "read")
                );

        final ObjectNode requestJson = new ObjectNode(JsonNodeFactory.instance)
                .set("permissions", permissions);

        final ObjectMapper objectMapper = new ObjectMapper();

        final PermissionsRequestDTO expected = PermissionsRequestDTO.builder()
                .addPermissions(PermissionDTO.builder()
                        .group("tests")
                        .name("read")
                        .build())
                .build();

        final PermissionsRequestDTO actual = objectMapper.convertValue(requestJson, PermissionsRequestDTO.class);

        assertThat(actual).isEqualTo(expected);
    }
}
