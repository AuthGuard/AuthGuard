package com.nexblocks.authguard.jwt.exchange;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
        get = {"is*", "get*"}, // Detect 'get' and 'is' prefixes in accessor methods
        jdkOnly = true, // Prevent the use of Guava's collections, Mapstruct doesn't like them
        validationMethod = Value.Style.ValidationMethod.NONE,
        unsafeDefaultAndDerived = true
)
public interface PkceParameters {
    PkceMode getMode();
    String getCodeChallenge();
    String getCodeChallengeMethod();
    String getCodeVerifier();

    enum PkceMode {
        AUTH_CODE_REQUEST,
        TOKEN_REQUEST
    }

    static PkceParameters forAuthCode(String codeChallenge,
                                      String codeChallengeMethod) {
        return ImmutablePkceParameters.builder()
                .mode(PkceMode.AUTH_CODE_REQUEST)
                .codeChallenge(codeChallenge)
                .codeChallengeMethod(codeChallengeMethod)
                .build();
    }

    static PkceParameters forToken(String codeVerifier) {
        return ImmutablePkceParameters.builder()
                .mode(PkceMode.TOKEN_REQUEST)
                .codeVerifier(codeVerifier)
                .build();
    }
}
