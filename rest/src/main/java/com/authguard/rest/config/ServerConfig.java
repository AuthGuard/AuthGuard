package com.authguard.rest.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(validationMethod = Value.Style.ValidationMethod.NONE)
@JsonSerialize(as = ImmutableServerConfig.class)
@JsonDeserialize(as = ImmutableServerConfig.class)
public interface ServerConfig {
    boolean enableSsl();
    boolean enforceSsl();
    boolean enableClientAuthentication();
    String getKeystorePath();
    String getKeystorePassword();
    String getTruststorePath();
    String getTruststorePassword();
    Integer getPort();
    Integer getSecurePort();
}
