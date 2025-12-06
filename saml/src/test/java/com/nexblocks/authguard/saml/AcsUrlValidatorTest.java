package com.nexblocks.authguard.saml;

import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcsUrlValidatorTest {

    @Test
    void isValidAcsUrl() {
        HttpUrl requestedUrl = HttpUrl.get("https://sp.com/callback");
        String registeredUtl = "https://sp.com/callback";

        assertThat(AcsUrlValidator.isValidAcsUrl(requestedUrl, registeredUtl))
                .isTrue();
    }

    @Test
    void isValidAcsUrlWithQueryParams() {
        HttpUrl requestedUrl = HttpUrl.get("https://sp.com/callback?query=1");
        String registeredUtl = "https://sp.com/callback";

        assertThat(AcsUrlValidator.isValidAcsUrl(requestedUrl, registeredUtl))
                .isTrue();
    }

    @Test
    void isValidAcsUrlDoesNotMatch() {
        HttpUrl requestedUrl = HttpUrl.get("https://other.com/callback");
        String registeredUtl = "https://sp.com/callback";

        assertThat(AcsUrlValidator.isValidAcsUrl(requestedUrl, registeredUtl))
                .isFalse();
    }

    @Test
    void isValidAcsUrlWrongSchema() {
        HttpUrl requestedUrl = HttpUrl.get("http://sp.com/callback");
        String registeredUtl = "https://sp.com/callback";

        assertThat(AcsUrlValidator.isValidAcsUrl(requestedUrl, registeredUtl))
                .isFalse();
    }
}