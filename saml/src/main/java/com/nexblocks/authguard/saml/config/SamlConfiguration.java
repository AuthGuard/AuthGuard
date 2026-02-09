package com.nexblocks.authguard.saml.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Collections;
import java.util.Set;

@Value.Immutable
@Value.Style(
        validationMethod = Value.Style.ValidationMethod.NONE,
        jdkOnly = true
)
@JsonSerialize(as = ImmutableSamlConfiguration.class)
@JsonDeserialize(as = ImmutableSamlConfiguration.class)
public interface SamlConfiguration {
    String getIssuer();
    boolean useUsername();
    boolean useEmail();
    boolean usePhoneNumber();
    String getCsrfTokenKey();
    ImmutableAttributesConfiguration responseAttributes();
    ImmutableMfaConfiguration useMultiFactorAuthentication();
    ImmutableSamlSignatures getSignature();
    ImmutableSamlSsoSession getSessions();

    @Value.Default
    default String getLoginPage() {
        return "resources/saml-sso-login.html";
    }

    @Value.Default
    default Set<String> getDomains() {
        return Collections.emptySet();
    }

    @Value.Immutable
    @Value.Style(
            validationMethod = Value.Style.ValidationMethod.NONE,
            jdkOnly = true
    )
    @JsonSerialize(as = ImmutableMfaConfiguration.class)
    @JsonDeserialize(as = ImmutableMfaConfiguration.class)
    interface MfaConfiguration {
        boolean useOtp();
    }

    @Value.Immutable
    @Value.Style(
            validationMethod = Value.Style.ValidationMethod.NONE,
            jdkOnly = true
    )
    @JsonSerialize(as = ImmutableAttributesConfiguration.class)
    @JsonDeserialize(as = ImmutableAttributesConfiguration.class)
    interface AttributesConfiguration {
        boolean includeUsername();
        boolean includeEmail();
        boolean includePhoneNumber();
    }

    @Value.Immutable
    @Value.Style(
            validationMethod = Value.Style.ValidationMethod.NONE,
            jdkOnly = true
    )
    @JsonSerialize(as = ImmutableSamlSignatures.class)
    @JsonDeserialize(as = ImmutableSamlSignatures.class)
    interface SamlSignatures {
        boolean useKeyStore();
        String getPrivateKey();
        String getPublicKey();
        String getKeyStorePath();
        String getKeyStoreKeyAlias();
        String getKeyStorePassword();

        /*
         * We only enable signing assertions by default since it's
         * the only requirement. Signing the response, or both is
         * optional.
         */
        @Value.Default
        default boolean signAssertion() {
            return true;
        }

        @Value.Default
        default boolean signResponse() {
            return false;
        }
    }

    @Value.Immutable
    @Value.Style(
            validationMethod = Value.Style.ValidationMethod.NONE,
            jdkOnly = true
    )
    @JsonSerialize(as = ImmutableSamlSsoSession.class)
    @JsonDeserialize(as = ImmutableSamlSsoSession.class)
    interface SamlSsoSession {
        String getLifetime();
        boolean allowRefresh();
    }
}
