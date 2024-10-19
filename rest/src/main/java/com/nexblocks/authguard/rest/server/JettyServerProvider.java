package com.nexblocks.authguard.rest.server;

import com.nexblocks.authguard.rest.config.ImmutableServerConfig;
import com.nexblocks.authguard.rest.exceptions.InitializationException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * May not be needed. Adds a lot of complexity and the trusted
 * certificates should be defined at system level anyway.
 */
@Deprecated
public class JettyServerProvider {
    private static final Logger LOG = LoggerFactory.getLogger(JettyServerProvider.class);

    private final ImmutableServerConfig serverConfig;

    public JettyServerProvider(final ImmutableServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public Server get() {
        final Server server = new Server();

        configureHttpConnector(server);

        if (serverConfig.enableSsl()) {
            configureHttpsConnector(server);
        }

        return server;
    }

    private void configureHttpConnector(final Server server) {
        final Integer port = Optional.ofNullable(serverConfig.getPort())
                .orElse(80);

        LOG.debug("Configuring HTTP connector for port {}", port);

        final ServerConnector connector = new ServerConnector(server);

        connector.setPort(port);

        server.addConnector(connector);
    }

    private void configureHttpsConnector(final Server server) {
        final Integer port = Optional.ofNullable(serverConfig.getSecurePort())
                .orElse(443);

        final SslContextFactory sslContextFactory = createContextFactory();
        final ServerConnector sslConnector = new ServerConnector(server);

        sslConnector.setPort(port);

        LOG.debug("Configuring HTTPS connector for port {}, with SSL context factory {}", port, sslContextFactory);

        server.addConnector(sslConnector);
    }

    private SslContextFactory createContextFactory() {
        if (serverConfig.getKeystorePath() == null) {
            throw new InitializationException("SSL is enabled but no keystore was provided");
        }

        final SslContextFactory sslContextFactory = new SslContextFactory.Server();

        configureKeystore(sslContextFactory);

        if (serverConfig.enableClientAuthentication()) {
            if (serverConfig.getTruststorePath() == null) {
                throw new InitializationException("Client authentication is enabled but no truststore was provided");
            }

            configureTruststore(sslContextFactory);
        }

        return sslContextFactory;
    }

    private String validatePathAndMakeAbsolute(final String pathStr) {
        final Path path = Paths.get(pathStr).toAbsolutePath();

        if (!Files.exists(path)) {
            throw new InitializationException("Path " + path.toString() + " doesn't exist");
        }

        return path.toString();
    }

    private void configureKeystore(final SslContextFactory sslContextFactory) {
        final String keystorePath = validatePathAndMakeAbsolute(serverConfig.getKeystorePath());

        sslContextFactory.setKeyStorePath(keystorePath);
        sslContextFactory.setKeyStorePassword(serverConfig.getKeystorePassword());
    }

    private void configureTruststore(final SslContextFactory sslContextFactory) {
        final String truststorePath = validatePathAndMakeAbsolute(serverConfig.getTruststorePath());

        sslContextFactory.setTrustStorePath(truststorePath);
        sslContextFactory.setTrustStorePassword(serverConfig.getTruststorePassword());
    }
}
