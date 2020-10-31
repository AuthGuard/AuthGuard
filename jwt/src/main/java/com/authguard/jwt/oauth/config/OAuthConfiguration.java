package com.authguard.jwt.oauth.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Style(
        validationMethod = Value.Style.ValidationMethod.NONE,
        jdkOnly = true
)
@JsonSerialize(as = ImmutableOAuthConfiguration.class)
@JsonDeserialize(as = ImmutableOAuthConfiguration.class)
public interface OAuthConfiguration {
    List<ImmutableOAuthClientConfiguration> getClients();
}
