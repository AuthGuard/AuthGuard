package com.nexblocks.authguard.saml;

import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.core.AuthnContext;

import static org.assertj.core.api.Assertions.assertThat;

class SamlConditionalAuthnTest {

    @Test
    void tokenSourceToClassRef() {
        assertThat(SamlConditionalAuthn.tokenSourceToClassRef("basic"))
                .isEqualTo(AuthnContext.PPT_AUTHN_CTX);

        assertThat(SamlConditionalAuthn.tokenSourceToClassRef("otp"))
                .isEqualTo(AuthnContext.TIME_SYNC_TOKEN_AUTHN_CTX);

        assertThat(SamlConditionalAuthn.tokenSourceToClassRef("totp"))
                .isEqualTo(AuthnContext.TIME_SYNC_TOKEN_AUTHN_CTX);
    }

    @Test
    void satisfiesRequestedContext() {
    }

    @Test
    void testSatisfiesRequestedContextAcceptAnyWhenUnset() {
        SamlAuthnRequest.AuthnClassRefs req = ImmutableAuthnClassRefs.builder()
                .unset(true)
                .build();

        assertThat(SamlConditionalAuthn.satisfiesRequestedContext(req, "any"))
                .isTrue();
    }

    @Test
    void testSatisfiesRequestedContextMatchExact() {
        SamlAuthnRequest.AuthnClassRefs req = ImmutableAuthnClassRefs.builder()
                .unset(false)
                .addClassRefs(AuthnContext.PPT_AUTHN_CTX)
                .comparison("exact")
                .build();

        assertThat(SamlConditionalAuthn.satisfiesRequestedContext(req, AuthnContext.PPT_AUTHN_CTX))
                .isTrue();

        // even if it's higher it still must reject it
        assertThat(SamlConditionalAuthn.satisfiesRequestedContext(req, AuthnContext.TIME_SYNC_TOKEN_AUTHN_CTX))
                .isFalse();
    }

    @Test
    void testSatisfiesRequestedContextMatchMinimum() {
        SamlAuthnRequest.AuthnClassRefs passwordCondition = ImmutableAuthnClassRefs.builder()
                .unset(false)
                .addClassRefs(AuthnContext.PPT_AUTHN_CTX)
                .comparison("minimum")
                .build();

        SamlAuthnRequest.AuthnClassRefs timeSyncTokenCondition = ImmutableAuthnClassRefs.builder()
                .unset(false)
                .addClassRefs(AuthnContext.TIME_SYNC_TOKEN_AUTHN_CTX)
                .comparison("minimum")
                .build();

        // both should be accepted
        assertThat(SamlConditionalAuthn.satisfiesRequestedContext(passwordCondition, AuthnContext.PPT_AUTHN_CTX))
                .isTrue();
        assertThat(SamlConditionalAuthn.satisfiesRequestedContext(passwordCondition, AuthnContext.TIME_SYNC_TOKEN_AUTHN_CTX))
                .isTrue();

        // only OTP or higher should be accepted
        assertThat(SamlConditionalAuthn.satisfiesRequestedContext(timeSyncTokenCondition, AuthnContext.PPT_AUTHN_CTX))
                .isFalse();
        assertThat(SamlConditionalAuthn.satisfiesRequestedContext(timeSyncTokenCondition, AuthnContext.TIME_SYNC_TOKEN_AUTHN_CTX))
                .isTrue();
    }

    @Test
    void testSatisfiesRequestedContextMatchBetter() {
        SamlAuthnRequest.AuthnClassRefs passwordCondition = ImmutableAuthnClassRefs.builder()
                .unset(false)
                .addClassRefs(AuthnContext.PPT_AUTHN_CTX)
                .comparison("better")
                .build();

        SamlAuthnRequest.AuthnClassRefs timeSyncTokenCondition = ImmutableAuthnClassRefs.builder()
                .unset(false)
                .addClassRefs(AuthnContext.TIME_SYNC_TOKEN_AUTHN_CTX)
                .comparison("better")
                .build();

        // only OTP is accepted
        assertThat(SamlConditionalAuthn.satisfiesRequestedContext(passwordCondition, AuthnContext.PPT_AUTHN_CTX))
                .isFalse();
        assertThat(SamlConditionalAuthn.satisfiesRequestedContext(passwordCondition, AuthnContext.TIME_SYNC_TOKEN_AUTHN_CTX))
                .isTrue();

        // neither should be accepted
        assertThat(SamlConditionalAuthn.satisfiesRequestedContext(timeSyncTokenCondition, AuthnContext.PPT_AUTHN_CTX))
                .isFalse();
        assertThat(SamlConditionalAuthn.satisfiesRequestedContext(timeSyncTokenCondition, AuthnContext.TIME_SYNC_TOKEN_AUTHN_CTX))
                .isFalse();
    }
}