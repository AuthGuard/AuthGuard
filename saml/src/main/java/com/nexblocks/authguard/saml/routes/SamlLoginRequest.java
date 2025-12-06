package com.nexblocks.authguard.saml.routes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableSamlLoginRequest.class)
@JsonDeserialize(as = ImmutableSamlLoginRequest.class)
@Value.Style(
        get = {"is*", "get*"}, // Detect 'get' and 'is' prefixes in accessor methods
        jdkOnly = true, // Prevent the use of Guava's collections, Mapstruct doesn't like them
        validationMethod = Value.Style.ValidationMethod.NONE,
        unsafeDefaultAndDerived = true
)
public interface SamlLoginRequest {
    String getIdentifier();
    String getPassword();
    String getRequestToken();
    String getTrackingSession();
}
