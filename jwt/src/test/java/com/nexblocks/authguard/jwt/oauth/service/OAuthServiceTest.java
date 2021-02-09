package com.nexblocks.authguard.jwt.oauth.service;

import com.nexblocks.authguard.jwt.oauth.TestIdentityServer;
import com.nexblocks.authguard.jwt.oauth.TokensResponse;
import com.nexblocks.authguard.jwt.oauth.config.ImmutableOAuthClientConfiguration;
import com.nexblocks.authguard.jwt.oauth.config.ImmutableOAuthConfiguration;
import com.nexblocks.authguard.jwt.oauth.util.HttpUrlAssertion;
import com.nexblocks.authguard.service.SessionsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.model.SessionBO;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OAuthServiceTest {
    private SessionsService sessionsService;
    private TestIdentityServer testIdentityServer;
    private ImmutableOAuthClientConfiguration clientConfiguration;

    private OAuthService oAuthService;

    @BeforeAll
    void setup() {
        testIdentityServer = new TestIdentityServer();

        testIdentityServer.start();

        clientConfiguration = ImmutableOAuthClientConfiguration.builder()
                .provider("test")
                .authUrl("http://localhost:" + testIdentityServer.getPort() + "/auth")
                .tokenUrl("http://localhost:" + testIdentityServer.getPort() + "/token")
                .authRedirectUrl("http://localhost/redirect")
                .tokenRedirectUrl("http://localhost/redirect")
                .clientId("unit-tests")
                .clientSecret("secret")
                .addDefaultScopes("openid", "profile")
                .build();

        final ImmutableOAuthConfiguration oAuthConfiguration = ImmutableOAuthConfiguration.builder()
                .addClients(clientConfiguration)
                .build();

        sessionsService = Mockito.mock(SessionsService.class);

        oAuthService = new OAuthService(oAuthConfiguration, sessionsService);
    }

    @AfterAll
    void destroy() {
        testIdentityServer.stop();
    }

    @BeforeEach
    void reset() {
        Mockito.reset(sessionsService);
    }

    @Test
    void getAuthorizationUrl() {
        Mockito.when(sessionsService.create(Mockito.any()))
                .thenAnswer(invocation -> invocation.getArgument(0, SessionBO.class).withSessionToken("state_token"));

        final HttpUrl actual = HttpUrl.get(oAuthService.getAuthorizationUrl("test").join());

        final HttpUrl expected = new HttpUrl.Builder()
                .scheme("http")
                .host("localhost")
                .port(testIdentityServer.getPort())
                .addPathSegment("auth")
                .addQueryParameter("client_id", clientConfiguration.getClientId())
                .addQueryParameter("redirect_uri", clientConfiguration.getAuthRedirectUrl())
                .addQueryParameter("response_type", "code")
                .addQueryParameter("scope", "openid profile")
                .addQueryParameter("state", "state_token")
                .build();

        HttpUrlAssertion.assertAuthorizationUrl(actual, expected, "nonce");

        Mockito.verify(sessionsService, Mockito.times(1)).create(Mockito.any());
    }

    @Test
    void getAuthorizationUrlInvalidProvider() {
        assertThatThrownBy(() -> oAuthService.getAuthorizationUrl("invalid").join())
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void exchangeAuthorizationCode() {
        Mockito.when(sessionsService.getByToken(Mockito.any()))
                .thenAnswer(invocation -> {
                    final SessionBO session = SessionBO.builder()
                            .sessionToken(invocation.getArgument(0))
                            .expiresAt(ZonedDateTime.now().plus(Duration.ofMinutes(2)))
                            .build();

                    return Optional.of(session);
                });

        final TokensResponse actual = oAuthService.exchangeAuthorizationCode("test", "random", "code")
                .join();
        final TokensResponse expected = testIdentityServer.getSuccessResponse();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void exchangeAuthorizationCodeInvalidState() {
        Mockito.when(sessionsService.getByToken(Mockito.any()))
                .thenAnswer(invocation -> Optional.empty());

        assertThatThrownBy(() -> oAuthService.exchangeAuthorizationCode("test", "random", "code")
                .join()).hasCauseInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void exchangeAuthorizationCodeExpiredState() {
        Mockito.when(sessionsService.getByToken(Mockito.any()))
                .thenAnswer(invocation -> {
                    final SessionBO session = SessionBO.builder()
                            .sessionToken(invocation.getArgument(0))
                            .expiresAt(ZonedDateTime.now().minus(Duration.ofMinutes(2)))
                            .build();

                    return Optional.of(session);
                });

        assertThatThrownBy(() -> oAuthService.exchangeAuthorizationCode("test", "random", "code")
                .join()).hasCauseInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void exchangeAuthorizationCodeInvalidProvider() {
        assertThatThrownBy(() -> oAuthService.getAuthorizationUrl("invalid").join())
                .isInstanceOf(ServiceException.class);
    }
}