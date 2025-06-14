package com.nexblocks.authguard.rest.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Set;

@Value.Immutable
@Value.Style(validationMethod = Value.Style.ValidationMethod.NONE, jdkOnly = true)
@JsonSerialize(as = ImmutableServerConfig.class)
@JsonDeserialize(as = ImmutableServerConfig.class)
public interface ServerConfig {
    boolean enableSsl();
    boolean enforceSsl();
    boolean enableClientAuthentication();

    @Deprecated
    String getKeystorePath();
    @Deprecated
    String getKeystorePassword();
    @Deprecated
    String getTruststorePath();
    @Deprecated
    String getTruststorePassword();

    Integer getPort();
    Integer getSecurePort();
    Set<String> getUnprotectedPaths();
    Integer getIdleConnectionTimeoutSeconds();
}
