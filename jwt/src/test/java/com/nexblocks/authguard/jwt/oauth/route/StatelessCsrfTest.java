package com.nexblocks.authguard.jwt.oauth.route;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StatelessCsrfTest {
    private static final String key = "JBSWY3DPEHPK3PXP";

    @Test
    void generateAndVerify() {
        StatelessCsrf statelessCsrf = new StatelessCsrf(key);
        String identifier = UUID.randomUUID().toString();

        String token = statelessCsrf.generate(identifier);
        boolean isValid = statelessCsrf.isValid(token, identifier);

        assertThat(isValid).isTrue();
    }

    @Test
    void generateAndVerifyWrongIdentifier() {
        StatelessCsrf statelessCsrf = new StatelessCsrf(key);
        String identifier = UUID.randomUUID().toString();
        String wrongIdentifier = UUID.randomUUID().toString();

        String token = statelessCsrf.generate(identifier);
        boolean isValid = statelessCsrf.isValid(token, wrongIdentifier);

        assertThat(isValid).isFalse();
    }

    @Test
    void generateAndVerifyInvalidBase64Token() {
        StatelessCsrf statelessCsrf = new StatelessCsrf(key);
        String identifier = UUID.randomUUID().toString();

        String token = statelessCsrf.generate(identifier);
        boolean isValid = statelessCsrf.isValid(token + "1", identifier);

        assertThat(isValid).isFalse();
    }

    @Test
    void generateAndVerifyWrongToken() {
        StatelessCsrf statelessCsrf = new StatelessCsrf(key);
        String identifier = UUID.randomUUID().toString();

        String token = statelessCsrf.generate(identifier);
        byte[] decoded = Base64.getUrlDecoder().decode(token);

        decoded[decoded.length - 1] = (byte) ((decoded[decoded.length - 1] + 1) % Byte.MAX_VALUE);

        String wrongToken = Base64.getUrlEncoder().encodeToString(decoded);

        boolean isValid = statelessCsrf.isValid(wrongToken, identifier);

        assertThat(isValid).isFalse();
    }
}
