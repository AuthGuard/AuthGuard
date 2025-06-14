package com.nexblocks.authguard.jwt.oauth;

import com.nexblocks.authguard.jwt.oauth.config.ImmutableOAuthClientConfiguration;
import com.nexblocks.authguard.jwt.oauth.util.HttpUrlAssertion;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OAuthServiceClientTest {
    private ImmutableOAuthClientConfiguration clientConfiguration;
    private OAuthServiceClient serviceClient;

    private TestIdentityServer testIdentityServer;

    @BeforeAll
    void setup() {
        testIdentityServer = new TestIdentityServer();
        testIdentityServer.start();

        clientConfiguration = ImmutableOAuthClientConfiguration.builder()
                .authUrl("http://localhost:" + testIdentityServer.getPort() + "/auth")
                .tokenUrl("http://localhost:" + testIdentityServer.getPort() + "/token")
                .authRedirectUrl("http://localhost/redirect")
                .tokenRedirectUrl("http://localhost/redirect")
                .clientId("unit-tests")
                .clientSecret("secret")
                .addDefaultScopes("openid", "profile")
                .build();

        serviceClient = new OAuthServiceClient(clientConfiguration);
    }

    @AfterAll
    void destroy() {
        testIdentityServer.stop();
    }

    @Test
    void createAuthorizationUrl() {
        final String actual = serviceClient.createAuthorizationUrl("random", ResponseType.CODE);
        final HttpUrl expected = new HttpUrl.Builder()
                .scheme("http")
                .host("localhost")
                .port(testIdentityServer.getPort())
                .addPathSegment("auth")
                .addQueryParameter("client_id", clientConfiguration.getClientId())
                .addQueryParameter("state", "random")
                .addQueryParameter("redirect_uri", clientConfiguration.getAuthRedirectUrl())
                .addQueryParameter("response_type", "code")
                .addQueryParameter("scope", "openid profile")
                .build();

        final HttpUrl parsed = HttpUrl.get(actual);

        HttpUrlAssertion.assertAuthorizationUrl(parsed, expected, "nonce");

        assertThat(parsed.queryParameter("nonce")).isNotNull();
    }

    @Test
    void authorizeSuccessful() {
        final TokensResponse expected = testIdentityServer.getSuccessResponse();
        final TokensResponse actual = serviceClient.authorize("code").subscribeAsCompletionStage().join();

        assertThat(actual).isEqualTo(expected);
    }
}