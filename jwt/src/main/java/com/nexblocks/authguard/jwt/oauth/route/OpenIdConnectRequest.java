package com.nexblocks.authguard.jwt.oauth.route;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableOpenIdConnectRequest.class)
@JsonDeserialize(as = ImmutableOpenIdConnectRequest.class)
@Value.Style(
        get = {"is*", "get*"}, // Detect 'get' and 'is' prefixes in accessor methods
        jdkOnly = true, // Prevent the use of Guava's collections, Mapstruct doesn't like them
        validationMethod = Value.Style.ValidationMethod.NONE,
        unsafeDefaultAndDerived = true
)
public interface OpenIdConnectRequest {
    // TODO this is a lot in one place, split it up
    String getResponseType();
    String getClientId();
    List<String> getScope();
    String getState();
    String getRedirectUri();

    String getIdentifier();
    String getPassword();
    String getDeviceId();
    String getExternalSessionId();
    String getUserAgent();
    String getSourceIp();

    String getCodeChallengeMethod();
    String getCodeChallenge();
    String getCodeVerifier();

    String getRequestToken();
}
