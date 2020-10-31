package com.authguard.jwt.oauth.service;

import com.authguard.dal.SessionsRepository;
import com.authguard.dal.model.SessionDO;
import com.authguard.jwt.oauth.TestIdentityServer;
import com.authguard.jwt.oauth.TokensResponse;
import com.authguard.jwt.oauth.config.ImmutableOAuthClientConfiguration;
import com.authguard.jwt.oauth.config.ImmutableOAuthConfiguration;
import com.authguard.jwt.oauth.util.HttpUrlAssertion;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.ServiceException;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OAuthServiceTest {
    private SessionsRepository sessionsRepository;
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

        sessionsRepository = Mockito.mock(SessionsRepository.class);

        oAuthService = new OAuthService(oAuthConfiguration, sessionsRepository);
    }

    @AfterAll
    void destroy() {
        testIdentityServer.stop();
    }

    @BeforeEach
    void reset() {
        Mockito.reset(sessionsRepository);
    }

    @Test
    void getAuthorizationUrl() {
        Mockito.when(sessionsRepository.save(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, SessionDO.class)));

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
                .build();

        HttpUrlAssertion.assertAuthorizationUrl(actual, expected, "state", "nonce");

        assertThat(actual.queryParameter("state"))
                .isNotNull()
                .hasSizeGreaterThan(10);

        Mockito.verify(sessionsRepository, Mockito.times(1)).save(Mockito.any());
    }

    @Test
    void getAuthorizationUrlInvalidProvider() {
        assertThatThrownBy(() -> oAuthService.getAuthorizationUrl("invalid").join())
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void exchangeAuthorizationCode() {
        Mockito.when(sessionsRepository.getById(Mockito.any()))
                .thenAnswer(invocation -> {
                    final SessionDO session = SessionDO.builder()
                            .id(invocation.getArgument(0))
                            .expiresAt(ZonedDateTime.now().plus(Duration.ofMinutes(2)))
                            .build();

                    return CompletableFuture.completedFuture(Optional.of(session));
                });

        final TokensResponse actual = oAuthService.exchangeAuthorizationCode("test", "random", "code")
                .join();
        final TokensResponse expected = testIdentityServer.getSuccessResponse();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void exchangeAuthorizationCodeInvalidState() {
        Mockito.when(sessionsRepository.getById(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> oAuthService.exchangeAuthorizationCode("test", "random", "code")
                .join()).hasCauseInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void exchangeAuthorizationCodeExpiredState() {
        Mockito.when(sessionsRepository.getById(Mockito.any()))
                .thenAnswer(invocation -> {
                    final SessionDO session = SessionDO.builder()
                            .id(invocation.getArgument(0))
                            .expiresAt(ZonedDateTime.now().minus(Duration.ofMinutes(2)))
                            .build();

                    return CompletableFuture.completedFuture(Optional.of(session));
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