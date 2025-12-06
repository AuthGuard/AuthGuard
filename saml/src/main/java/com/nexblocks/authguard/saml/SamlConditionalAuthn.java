package com.nexblocks.authguard.saml;

import org.opensaml.saml.saml2.core.AuthnContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SamlConditionalAuthn {

    private static final Map<String,Integer> STRENGTH = Map.ofEntries(
            Map.entry(AuthnContext.PASSWORD_AUTHN_CTX,              1),
            Map.entry(AuthnContext.PPT_AUTHN_CTX,                   2),
            Map.entry(AuthnContext.TIME_SYNC_TOKEN_AUTHN_CTX,       3)
    );

    public static int rankSupported(final String urn) {
        /*
         * We default supported rank to zero so that if a value isn't mapped
         * then it should have the lowest rank, and the tests will fail
         */
        return STRENGTH.getOrDefault(urn, 0);
    }

    public static int rankRequested(final String urn) {
        /*
         * We default requested rank to 50 so that if a value isn't mapped
         * then it should have the highest rank. It's safer to reject authn
         * requested when we can't rank the strength than accept one when
         * we shouldn't.
         */
        return STRENGTH.getOrDefault(urn, 50);
    }

    public static String tokenSourceToClassRef(final String source) {
        return switch (source) {
            case "basic" -> AuthnContext.PPT_AUTHN_CTX;
            case "otp", "totp" -> AuthnContext.TIME_SYNC_TOKEN_AUTHN_CTX;
            default -> AuthnContext.UNSPECIFIED_AUTHN_CTX;
        };
    }

    public static boolean satisfiesRequestedContext(final SamlAuthnRequest authnRequest,
                                                    final String tokenSource) {
        SamlAuthnRequest.AuthnClassRefs rac = authnRequest.getAuthnClassRefs();
        String tokenSourceClassRef = SamlConditionalAuthn.tokenSourceToClassRef(tokenSource);

        if (!(rac == null || rac.isUnset())) {
            return SamlConditionalAuthn.satisfiesRequestedContext(rac, tokenSourceClassRef);
        }

        return true;
    }

    public static boolean satisfiesRequestedContext(final SamlAuthnRequest.AuthnClassRefs req,
                                                    final String actualClassRef) {
        if (req == null || req.isUnset() || req.getClassRefs() == null || req.getClassRefs().isEmpty()) {
            return true;
        }

        String comparison = Optional.ofNullable(req.getComparison()).orElse("minimum").toLowerCase();
        List<String> requested = req.getClassRefs();

        int actual = rankSupported(actualClassRef);
        int minReq = requested.stream().mapToInt(SamlConditionalAuthn::rankRequested).min().orElse(0);
        int maxReq = requested.stream().mapToInt(SamlConditionalAuthn::rankRequested).max().orElse(0);

        return switch (comparison) {
            case "exact" -> requested.stream().anyMatch(r -> r.equals(actualClassRef));
            case "minimum" -> actual >= minReq;
            case "maximum" -> actual <= maxReq;
            case "better" -> actual > maxReq;
            default -> requested.stream().anyMatch(r -> r.equals(actualClassRef));
        };
    }
}
