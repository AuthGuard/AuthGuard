package com.nexblocks.authguard.saml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Style(
        get = { "is*", "get*" }, // Detect 'get' and 'is' prefixes in accessor methods
        jdkOnly = true, // Prevent the use of Guava's collections, Mapstruct doesn't like them
        validationMethod = Value.Style.ValidationMethod.NONE,
        unsafeDefaultAndDerived = true
)
public interface SamlAuthnRequest {
    String getRequestId();
    String getIssuer();
    String getAcsUrl();
    ProtocolBinding getProtocolBinding();
    boolean isForceAuthn();
    boolean isPassive();
    String getRelayState();
    boolean isValid();
    String getErrorMessage();
    AuthnClassRefs getAuthnClassRefs();
    NameIdFormat getNameIdFormat();
    ImmutableServerSideDetails getServerSideDetails();

    @Value.Immutable
    @Value.Style(
            get = { "is*", "get*" },
            jdkOnly = true,
            validationMethod = Value.Style.ValidationMethod.NONE
    )
    interface AuthnClassRefs {
        boolean isUnset();
        List<String> getClassRefs();
        String getComparison();
    }

    @Value.Immutable
    @Value.Style(
            get = { "is*", "get*" },
            jdkOnly = true,
            validationMethod = Value.Style.ValidationMethod.NONE
    )
    interface ServerSideDetails {
        String getClientId();
    }

    enum NameIdFormat {
        TRANSIENT,
        PERSISTENT,
        EMAIL_ADDRESS,
        UNSPECIFIED,
    }

    enum ProtocolBinding {
        HTTP_POST,
        OTHER
    }
}
