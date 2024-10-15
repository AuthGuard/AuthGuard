package com.nexblocks.authguard.jwt.exchange;

import com.google.common.collect.ImmutableMap;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PkceVerifierTest {

    @Test
    void verifyNonPkce() {
        AccountTokenDO accountToken = AccountTokenDO.builder().build();
        AuthRequestBO request = AuthRequestBO.builder().build();

        assertThat(PkceVerifier.verifyIfPkce(accountToken, request).isSuccess())
                .isTrue();
    }

    @Test
    void verifyPkce() {
        String verifier = "Hello, world!";
        String challenge = "315f5bdb76d078c43b8ac0064e4a0164612b1fce77c869345bfc94c75894edd3";

        AccountTokenDO accountToken = AccountTokenDO.builder()
                .additionalInformation(ImmutableMap.of("codeChallenge", challenge, "codeChallengeMethod", "S256"))
                .build();
        AuthRequestBO request = AuthRequestBO.builder()
                .extraParameters(PkceParameters.forToken(verifier))
                .build();

        assertThat(PkceVerifier.verifyIfPkce(accountToken, request).isSuccess())
                .isTrue();
    }

    @Test
    void verifyPkceMismatch() {
        String verifier = "Different";
        String challenge = "315f5bdb76d078c43b8ac0064e4a0164612b1fce77c869345bfc94c75894edd3";

        AccountTokenDO accountToken = AccountTokenDO.builder()
                .additionalInformation(ImmutableMap.of("codeChallenge", challenge, "codeChallengeMethod", "S256"))
                .build();
        AuthRequestBO request = AuthRequestBO.builder()
                .extraParameters(PkceParameters.forToken(verifier))
                .build();

        assertThat(PkceVerifier.verifyIfPkce(accountToken, request).isFailure())
                .isTrue();
    }

    @Test
    void verifyIfPkcePkceRequestNonPkceToken() {
        AccountTokenDO accountToken = AccountTokenDO.builder().build();
        AuthRequestBO request = AuthRequestBO.builder()
                .extraParameters(PkceParameters.forToken("verifier"))
                .build();

        assertThat(PkceVerifier.verifyIfPkce(accountToken, request).isFailure())
                .isTrue();
    }

    @Test
    void verifyIfPkcePkceTokenNonPkceRequest() {
        AccountTokenDO accountToken = AccountTokenDO.builder()
                .additionalInformation(ImmutableMap.of("codeChallenge", "challenge", "codeChallengeMethod", "S256"))
                .build();
        AuthRequestBO request = AuthRequestBO.builder().build();

        assertThat(PkceVerifier.verifyIfPkce(accountToken, request).isFailure())
                .isTrue();
    }

    @Test
    void verifyIfPkceMissingChallenge() {
        AccountTokenDO accountToken = AccountTokenDO.builder()
                .additionalInformation(ImmutableMap.of("codeChallengeMethod", "S256"))
                .build();
        AuthRequestBO request = AuthRequestBO.builder()
                .extraParameters(PkceParameters.forToken("verifier"))
                .build();

        assertThat(PkceVerifier.verifyIfPkce(accountToken, request).isFailure())
                .isTrue();
    }

    @Test
    void verifyIfPkceMissingMethod() {
        AccountTokenDO accountToken = AccountTokenDO.builder()
                .additionalInformation(ImmutableMap.of("codeChallengeMethod", "S256"))
                .build();
        AuthRequestBO request = AuthRequestBO.builder()
                .extraParameters(PkceParameters.forToken("verifier"))
                .build();

        assertThat(PkceVerifier.verifyIfPkce(accountToken, request).isFailure())
                .isTrue();
    }
}