package com.nexblocks.authguard.rest;

import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.config.JacksonConfigContext;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class AuthGuardCliTest {
    private ConfigurationLoader configurationLoader;
    private ServerRunner serverRunner;
    private final ConfigContext configContext = new JacksonConfigContext(new ObjectNode(JsonNodeFactory.instance));

    private AuthGuardCli cli;

    @BeforeEach
    void setup() {
        configurationLoader = Mockito.mock(ConfigurationLoader.class);
        serverRunner = Mockito.mock(ServerRunner.class);

        Mockito.when(configurationLoader.loadFromFile(Mockito.any()))
                .thenReturn(configContext);
        Mockito.when(configurationLoader.loadFromResources())
                .thenReturn(configContext);

        cli = new AuthGuardCli(configurationLoader, serverRunner);
    }

    @Test
    void executeConfigShortOption() {
        final String[] args = new String[] { "-c", "/path/to/file.yaml" };

        final int exitCode = cli.execute(args);

        assertThat(exitCode).isEqualTo(0);

        Mockito.verify(configurationLoader).loadFromFile("/path/to/file.yaml");
        Mockito.verify(serverRunner).run(configContext);
    }

    @Test
    void executeConfigLongOption() {
        final String[] args = new String[] { "--config", "/path/to/file.yaml" };

        final int exitCode = cli.execute(args);

        assertThat(exitCode).isEqualTo(0);

        Mockito.verify(configurationLoader).loadFromFile("/path/to/file.yaml");
        Mockito.verify(serverRunner).run(configContext);
    }

    @Test
    void executeNoConfigOption() {
        final String[] args = new String[] { };

        final int exitCode = cli.execute(args);

        assertThat(exitCode).isEqualTo(0);

        Mockito.verify(configurationLoader).loadFromResources();
    }

    @Test
    void executeHelpShortOption() {
        final String[] args = new String[] { "-h" };

        final int exitCode = cli.execute(args);

        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void executeHelpLongOption() {
        final String[] args = new String[] { "--help" };

        final int exitCode = cli.execute(args);

        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void executeUnknownOption() {
        final String[] args = new String[] { "-unknown" };

        final int exitCode = cli.execute(args);

        assertThat(exitCode).isEqualTo(1);
    }
}