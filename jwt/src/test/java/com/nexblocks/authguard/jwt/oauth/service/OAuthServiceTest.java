package com.nexblocks.authguard.jwt.oauth.service;

import com.nexblocks.authguard.jwt.oauth.TestIdentityServer;
import com.nexblocks.authguard.jwt.oauth.TokensResponse;
import com.nexblocks.authguard.jwt.oauth.config.ImmutableOAuthClientConfiguration;
import com.nexblocks.authguard.jwt.oauth.config.ImmutableOAuthConfiguration;
import com.nexblocks.authguard.jwt.oauth.util.HttpUrlAssertion;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.SessionsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.model.SessionBO;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OAuthServiceTest {
    private SessionsService sessionsService;
    private AccountsService accountsService;
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

        ImmutableOAuthClientConfiguration accountProviderClientConfiguration = ImmutableOAuthClientConfiguration.builder()
                .provider("account_test")
                .authUrl("http://localhost:" + testIdentityServer.getPort() + "/auth")
                .tokenUrl("http://localhost:" + testIdentityServer.getPort() + "/token")
                .authRedirectUrl("http://localhost/redirect")
                .tokenRedirectUrl("http://localhost/redirect")
                .clientId("unit-tests")
                .clientSecret("secret")
                .addDefaultScopes("openid", "profile")
                .accountProvider(true)
                .emailField("email")
                .build();

         ImmutableOAuthConfiguration oAuthConfiguration = ImmutableOAuthConfiguration.builder()
                .addClients(clientConfiguration)
                .addClients(accountProviderClientConfiguration)
                .build();

        sessionsService = Mockito.mock(SessionsService.class);
        accountsService = Mockito.mock(AccountsService.class);

        oAuthService = new OAuthService(oAuthConfiguration, sessionsService, accountsService);
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
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, SessionBO.class).withSessionToken("state_token")));

         HttpUrl actual = HttpUrl.get(oAuthService.getAuthorizationUrl("test").subscribeAsCompletionStage().join());

         HttpUrl expected = new HttpUrl.Builder()
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
        assertThatThrownBy(() -> oAuthService.getAuthorizationUrl("invalid").subscribeAsCompletionStage().join())
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void exchangeAuthorizationCode() {
        Mockito.when(sessionsService.getByToken(Mockito.any()))
                .thenAnswer(invocation -> {
                    SessionBO session = SessionBO.builder()
                            .sessionToken(invocation.getArgument(0))
                            .expiresAt(Instant.now().plus(Duration.ofMinutes(2)))
                            .build();

                    return Uni.createFrom().item(Optional.of(session));
                });

         TokensResponse actual = oAuthService.exchangeAuthorizationCode("test", "random", "code")
                .subscribeAsCompletionStage().join();
         TokensResponse expected = testIdentityServer.getSuccessResponse();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void exchangeAuthorizationCodeInvalidState() {
        Mockito.when(sessionsService.getByToken(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(Optional.empty()));

        assertThatThrownBy(() -> oAuthService.exchangeAuthorizationCode("test", "random", "code")
                .subscribeAsCompletionStage().join()).hasCauseInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void exchangeAuthorizationCodeExpiredState() {
        Mockito.when(sessionsService.getByToken(Mockito.any()))
                .thenAnswer(invocation -> {
                     SessionBO session = SessionBO.builder()
                            .sessionToken(invocation.getArgument(0))
                            .expiresAt(Instant.now().minus(Duration.ofMinutes(2)))
                            .build();

                    return Uni.createFrom().item(Optional.of(session));
                });

        assertThatThrownBy(() -> oAuthService.exchangeAuthorizationCode("test", "random", "code")
                .subscribeAsCompletionStage().join()).hasCauseInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void exchangeAuthorizationCodeInvalidProvider() {
        assertThatThrownBy(() -> oAuthService.getAuthorizationUrl("invalid").subscribeAsCompletionStage().join())
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void exchangeAuthorizationCodeAndCreateAccount() {
         RequestContextBO expectedContext = RequestContextBO.builder()
                .idempotentKey("code")
                .source("account_test").build();

        Mockito.when(sessionsService.getByToken(Mockito.any()))
                .thenAnswer(invocation -> {
                     SessionBO session = SessionBO.builder()
                            .sessionToken(invocation.getArgument(0))
                            .expiresAt(Instant.now().plus(Duration.ofMinutes(2)))
                            .build();

                    return Uni.createFrom().item(Optional.of(session));
                });

        Mockito.when(accountsService.create(Mockito.any(), Mockito.eq(expectedContext)))
                .thenAnswer(invocation -> {
                    AccountBO argWithId = invocation.getArgument(0, AccountBO.class).withId(1);
                    return Uni.createFrom().item(argWithId);
                });

        Mockito.when(accountsService.getByExternalIdUnchecked(Mockito.any()))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

         TokensResponse actual = oAuthService.exchangeAuthorizationCode("account_test", "random", "code")
                .subscribeAsCompletionStage().join();
         TokensResponse expected = testIdentityServer.getSuccessResponse();

        expected.setAccountId(1);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void exchangeAuthorizationCodeAndGetAccount() {
        Mockito.when(sessionsService.getByToken(Mockito.any()))
                .thenAnswer(invocation -> {
                     SessionBO session = SessionBO.builder()
                            .sessionToken(invocation.getArgument(0))
                            .expiresAt(Instant.now().plus(Duration.ofMinutes(2)))
                            .build();

                    return Uni.createFrom().item(Optional.of(session));
                });

        Mockito.when(accountsService.getByExternalIdUnchecked("1"))
                .thenReturn(Uni.createFrom().item(Optional.of(AccountBO.builder().id(1).build())));

         TokensResponse actual = oAuthService.exchangeAuthorizationCode("account_test", "random", "code")
                .subscribeAsCompletionStage().join();
         TokensResponse expected = testIdentityServer.getSuccessResponse();

        expected.setAccountId(1);

        assertThat(actual).isEqualTo(expected);
    }
}