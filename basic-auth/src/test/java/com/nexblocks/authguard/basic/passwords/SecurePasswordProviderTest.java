package com.nexblocks.authguard.basic.passwords;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.config.JacksonConfigContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurePasswordProviderTest {

    @Test
    void parsesWithoutPreviousVersions() {
        final ObjectNode configRoot = new ObjectNode(JsonNodeFactory.instance)
                .put("algorithm", "bcrypt")
                .put("validFor", "5d");

        final ObjectNode bcryptConfig = new ObjectNode(JsonNodeFactory.instance);
        configRoot.set("bcrypt", bcryptConfig);

        final ConfigContext configContext = new JacksonConfigContext(configRoot);

        final SecurePasswordProvider provider = new SecurePasswordProvider(configContext);

        assertThat(provider.getCurrentVersion()).isEqualTo(1);
    }

    @Test
    void parsesWithPreviousVersions() {
        final ObjectNode configRoot = new ObjectNode(JsonNodeFactory.instance)
                .put("algorithm", "bcrypt")
                .put("validFor", "5d")
                .put("version", 2);

        final ObjectNode bcryptConfig = new ObjectNode(JsonNodeFactory.instance);
        configRoot.set("bcrypt", bcryptConfig);

        final ObjectNode previousPasswordConfig = new ObjectNode(JsonNodeFactory.instance)
                .put("algorithm", "scrypt")
                .put("validFor", "5d")
                .put("version",1);

        final ObjectNode scryptConfig = new ObjectNode(JsonNodeFactory.instance);
        previousPasswordConfig.set("scrypt", scryptConfig);

        final ArrayNode previousVersions = new ArrayNode(JsonNodeFactory.instance)
                .add(previousPasswordConfig);

        configRoot.set("previousVersions", previousVersions);

        final ConfigContext configContext = new JacksonConfigContext(configRoot);

        final SecurePasswordProvider provider = new SecurePasswordProvider(configContext);

        assertThat(provider.getCurrentVersion()).isEqualTo(2);
        assertThat(provider.getPreviousVersions().get(1)).isInstanceOf(SCryptPassword.class);
    }
}