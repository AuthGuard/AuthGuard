package com.nexblocks.authguard.rest.server;

import com.nexblocks.authguard.rest.config.ImmutableServerConfig;
import com.nexblocks.authguard.rest.exceptions.InitializationException;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JettyServerProviderTest {

//    @Test
    void getNoSslEnabled() {
        final ImmutableServerConfig config = ImmutableServerConfig.builder().build();
        final JettyServerProvider provider = new JettyServerProvider(config);

        final Server server = provider.get();

        assertThat(server.getConnectors())
                .hasSize(1)
                .allMatch(connector -> connector.getProtocols().size() == 1 && connector.getProtocols().get(0).equals("http/1.1"));
    }

//    @Test
    void getSslEnabled() {
        final ImmutableServerConfig config = ImmutableServerConfig.builder()
                .enableSsl(true)
                .keystorePath("src/test/resources/keystore")
                .keystorePassword("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4") // taken from the Jetty example
                .build();
        final JettyServerProvider provider = new JettyServerProvider(config);

        final Server server = provider.get();

        assertThat(server.getConnectors())
                .anyMatch(connector -> connector.getProtocols().stream().anyMatch("ssl"::equals));
    }

//    @Test
    void getSslEnabledNoKeystore() {
        final ImmutableServerConfig config = ImmutableServerConfig.builder()
                .enableSsl(true)
                .build();
        final JettyServerProvider provider = new JettyServerProvider(config);

        assertThatThrownBy(provider::get).isInstanceOf(InitializationException.class);
    }
}