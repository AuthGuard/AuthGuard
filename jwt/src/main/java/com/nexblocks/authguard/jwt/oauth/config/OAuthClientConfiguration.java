package com.nexblocks.authguard.jwt.oauth.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Set;

@Value.Immutable
@Value.Style(
        get = { "get*", "is*" },
        validationMethod = Value.Style.ValidationMethod.NONE,
        jdkOnly = true
)
@JsonSerialize(as = ImmutableOAuthClientConfiguration.class)
@JsonDeserialize(as = ImmutableOAuthClientConfiguration.class)
public interface OAuthClientConfiguration {
    /**
     * The name of the identity provider. Used to identify
     * the client.
     */
    String getProvider();

    /**
     * The URL to call to get an authorization code.
     */
    String getAuthUrl();

    /**
     * The URL to call to exchange the authorization code
     * with ID and access tokens.
     */
    String getTokenUrl();

    /**
     * The URL to give to the identity provider to redirect
     * the user to after authorization.
     */
    String getAuthRedirectUrl();

    /**
     * The URL to give to the identity provider to redirect
     * the user after authorization code exchange.
     */
    String getTokenRedirectUrl();

    String getClientId();
    String getClientSecret();
    Set<String> getDefaultScopes();

    /**
     * If true, the client will create an account from an ID token
     * if none exists, or will return the account ID in the process.
     */
    boolean isAccountProvider();
    String getEmailField();
}
