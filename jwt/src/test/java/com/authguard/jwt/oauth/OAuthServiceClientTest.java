package com.authguard.jwt.oauth;

import com.authguard.jwt.oauth.config.ImmutableOAuthClientConfiguration;
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

        assertThat(parsed.scheme()).isEqualTo(expected.scheme());
        assertThat(parsed.host()).isEqualTo(expected.host());
        assertThat(parsed.port()).isEqualTo(expected.port());
        assertThat(parsed.pathSegments()).isEqualTo(expected.pathSegments());
        assertThat(parsed.queryParameter("client_id"))
                .isEqualTo(expected.queryParameter("client_id"));
        assertThat(parsed.queryParameter("redirect_uri"))
                .isEqualTo(expected.queryParameter("redirect_uri"));
        assertThat(parsed.queryParameter("state"))
                .isEqualTo(expected.queryParameter("state"));
        assertThat(parsed.queryParameter("response_type"))
                .isEqualTo(expected.queryParameter("response_type"));
        assertThat(parsed.queryParameter("scope"))
                .isEqualTo(expected.queryParameter("scope"));

        assertThat(parsed.queryParameter("nonce")).isNotNull();
    }

    @Test
    void authorizeSuccessful() {
        final TokensResponse expected = testIdentityServer.getSuccessResponse();
        final TokensResponse actual = serviceClient.authorize("code").join();

        assertThat(actual).isEqualTo(expected);
    }
}