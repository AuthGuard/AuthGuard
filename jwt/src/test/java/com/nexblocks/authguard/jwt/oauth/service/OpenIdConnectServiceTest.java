package com.nexblocks.authguard.jwt.oauth.service;

import com.google.common.collect.ImmutableMap;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.jwt.exchange.PkceParameters;
import com.nexblocks.authguard.jwt.oauth.route.ImmutableOpenIdConnectRequest;
import com.nexblocks.authguard.jwt.oauth.route.OpenIdConnectRequest;
import com.nexblocks.authguard.service.ClientsService;
import com.nexblocks.authguard.service.ExchangeService;
import com.nexblocks.authguard.service.TrackingSessionsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.*;
import io.smallrye.mutiny.Uni;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import io.smallrye.mutiny.Uni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenIdConnectServiceTest {

    private final ImmutableOpenIdConnectRequest request = ImmutableOpenIdConnectRequest.builder()
            .responseType("code")
            .clientId("1")
            .redirectUri("http://test-domain.com/oidc/login")
            .identifier("user")
            .password("password")
            .build();

    private final ClientBO client = ClientBO.builder()
            .id(1)
            .domain("test")
            .clientType(Client.ClientType.SSO)
            .baseUrl("test-domain.com")
            .build();

    private final RequestContextBO context = RequestContextBO.builder().build();

    private ExchangeService exchangeService;
    private ClientsService clientsService;
    private TrackingSessionsService trackingSessionsService;
    private AccountTokensRepository accountTokensRepository;
    private OpenIdConnectService openIdConnectService;

    @BeforeEach
    void setup() {
        exchangeService = Mockito.mock(ExchangeService.class);
        clientsService = Mockito.mock(ClientsService.class);
        trackingSessionsService = Mockito.mock(TrackingSessionsService.class);
        accountTokensRepository = Mockito.mock(AccountTokensRepository.class);

        Mockito.when(trackingSessionsService.startSession(Mockito.any()))
                .thenReturn(Uni.createFrom().item(SessionBO.builder()
                        .sessionToken("tracking-session")
                        .build()));

        openIdConnectService = new OpenIdConnectService(clientsService, exchangeService,
                trackingSessionsService, accountTokensRepository);
    }

    @Test
    void getRequestFromToken() {
        OpenIdConnectRequest fullRequest = request
                .withState("state")
                .withScope("scope-1")
                .withCodeChallenge("random-code-challenge")
                .withCodeChallengeMethod("S256")
                .withIdentifier(null)
                .withPassword(null);

        AccountTokenDO token = AccountTokenDO.builder()
                .domain("main")
                .token("test-token")
                .userAgent("test-client")
                .clientId("1")
                .additionalInformation(ImmutableMap.<String, String>builder()
                        .put(OAuthConst.Params.ResponseType, fullRequest.getResponseType())
                        .put(OAuthConst.Params.RedirectUri, fullRequest.getRedirectUri())
                        .put(OAuthConst.Params.State, fullRequest.getState())
                        .put(OAuthConst.Params.Scope, String.join(",", fullRequest.getScope()))
                        .put(OAuthConst.Params.CodeChallengeMethod, fullRequest.getCodeChallengeMethod())
                        .put(OAuthConst.Params.CodeChallenge, fullRequest.getCodeChallenge())
                        .build())
                .build();
        
        RequestContextBO requestContext = RequestContextBO.builder()
                .userAgent("test-client")
                .build();
        
        Mockito.when(accountTokensRepository.getByToken(token.getToken()))
                .thenReturn(Uni.createFrom().item(Optional.of(token)));

        OpenIdConnectRequest retrievedRequest =
                openIdConnectService.getRequestFromToken(token.getToken(), requestContext, "main").subscribeAsCompletionStage().join();
        
        assertThat(retrievedRequest).isEqualTo(fullRequest);
    }

    @Test
    void getRequestFromTokenDifferentDomain() {
        AccountTokenDO token = AccountTokenDO.builder()
                .domain("main")
                .token("test-token")
                .userAgent("test-client")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder()
                .userAgent("other-client")
                .build();

        Mockito.when(accountTokensRepository.getByToken(token.getToken()))
                .thenReturn(Uni.createFrom().item(Optional.of(token)));

        assertThatThrownBy(() -> openIdConnectService.getRequestFromToken(token.getToken(), requestContext, "main").subscribeAsCompletionStage().join())
                .hasCauseInstanceOf(ServiceAuthorizationException.class)
                .cause()
                .extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.GENERIC_AUTH_FAILURE.getCode());
    }

    @Test
    void getRequestFromTokenDifferentUserAgent() {
        AccountTokenDO token = AccountTokenDO.builder()
                .domain("main")
                .token("test-token")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder()
                .userAgent("test-client")
                .build();

        Mockito.when(accountTokensRepository.getByToken(token.getToken()))
                .thenReturn(Uni.createFrom().item(Optional.of(token)));

        assertThatThrownBy(() -> openIdConnectService.getRequestFromToken(token.getToken(), requestContext, "other").subscribeAsCompletionStage().join())
                .hasCauseInstanceOf(ServiceNotFoundException.class)
                .cause()
                .extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN.getCode());
    }

    @Test
    void processAuth() {
        OpenIdConnectRequest request = ImmutableOpenIdConnectRequest.builder()
                .responseType("code")
                .clientId("1")
                .redirectUri("http://test-domain.com/oidc/login")
                .identifier("user")
                .password("password")
                .build();

        RequestContextBO context = RequestContextBO.builder()
                .clientId("1")
                .build();

        AuthRequestBO expectedRequest = AuthRequestBO.builder()
                .domain(client.getDomain())
                .identifier(request.getIdentifier())
                .password(request.getPassword())
                .externalSessionId(request.getExternalSessionId())
                .build();
        AuthResponseBO expectedResponse = AuthResponseBO.builder()
                .token("auth code")
                .build();

        Mockito.when(clientsService.getById(1, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));

        Mockito.when(exchangeService.exchange(expectedRequest, "basic", "authorizationCode", context))
                .thenReturn(Uni.createFrom().item(expectedResponse));

        assertThat(openIdConnectService.processAuth(request, context, "main").subscribeAsCompletionStage().join())
                .isEqualTo(expectedResponse);
    }

    @Test
    void processAuthInvalidClient() {
        OpenIdConnectRequest request = ImmutableOpenIdConnectRequest.builder()
                .responseType("code")
                .clientId("1")
                .redirectUri("http://test-domain.com/oidc/login")
                .identifier("user")
                .password("password")
                .build();

        RequestContextBO context = RequestContextBO.builder().build();

        Mockito.when(clientsService.getById(1, "main"))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        assertThatThrownBy(() -> openIdConnectService.processAuth(request, context, "main").subscribeAsCompletionStage().join())
                .hasCauseInstanceOf(ServiceException.class)
                .cause()
                .extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.APP_DOES_NOT_EXIST.getCode());
    }

    @Test
    void processAuthNonSsoClient() {
        ClientBO client = ClientBO.builder()
                .domain("test")
                .clientType(Client.ClientType.AUTH)
                .build();

        OpenIdConnectRequest request = ImmutableOpenIdConnectRequest.builder()
                .responseType("code")
                .clientId("1")
                .redirectUri("http://test-domain.com/oidc/login")
                .identifier("user")
                .password("password")
                .build();

        RequestContextBO context = RequestContextBO.builder().build();

        Mockito.when(clientsService.getById(1, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));

        assertThatThrownBy(() -> openIdConnectService.processAuth(request, context, "main").subscribeAsCompletionStage().join())
                .hasCauseInstanceOf(ServiceException.class)
                .cause()
                .extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.CLIENT_NOT_PERMITTED.getCode());
    }

    @Test
    void processAuthInvalidRedirectUri() {
        ClientBO client = ClientBO.builder()
                .domain("test")
                .clientType(Client.ClientType.SSO)
                .build();

        OpenIdConnectRequest request = ImmutableOpenIdConnectRequest.builder()
                .responseType("code")
                .clientId("1")
                .redirectUri("invalid")
                .identifier("user")
                .password("password")
                .build();

        RequestContextBO context = RequestContextBO.builder().build();

        Mockito.when(clientsService.getById(1, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));

        AbstractThrowableAssert<?, ?> cause = assertThatThrownBy(() -> openIdConnectService.processAuth(request, context, "main").subscribeAsCompletionStage().join())
                .hasCauseInstanceOf(ServiceException.class)
                .cause();

        cause.extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.GENERIC_AUTH_FAILURE.getCode());

        cause.extracting(Throwable::getMessage)
                .isEqualTo("Invalid redirect URL");
    }

    @Test
    void processAuthRedirectUrlDifferentHost() {
        ClientBO client = ClientBO.builder()
                .domain("test")
                .clientType(Client.ClientType.SSO)
                .baseUrl("test-domain.com")
                .build();

        OpenIdConnectRequest request = ImmutableOpenIdConnectRequest.builder()
                .responseType("code")
                .clientId("1")
                .redirectUri("http://test.com/oidc/login")
                .identifier("user")
                .password("password")
                .build();

        RequestContextBO context = RequestContextBO.builder().build();

        Mockito.when(clientsService.getById(1, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));

        AbstractThrowableAssert<?, ?> cause = assertThatThrownBy(() -> openIdConnectService.processAuth(request, context, "main").subscribeAsCompletionStage().join())
                .hasCauseInstanceOf(ServiceException.class)
                .cause();

        cause.extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.GENERIC_AUTH_FAILURE.getCode());

        cause.extracting(Throwable::getMessage)
                .isEqualTo("Redirect URL doesn't match the client base URL");
    }

    @Test
    void processAuthRedirectUrlDifferentHostEmptyBaseUrl() {
        ClientBO client = ClientBO.builder()
                .domain("test")
                .clientType(Client.ClientType.SSO)
                .baseUrl("")
                .build();

        OpenIdConnectRequest request = ImmutableOpenIdConnectRequest.builder()
                .responseType("code")
                .clientId("1")
                .redirectUri("http://test.com/oidc/login")
                .identifier("user")
                .password("password")
                .build();

        RequestContextBO context = RequestContextBO.builder().build();

        Mockito.when(clientsService.getById(1, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));

        AbstractThrowableAssert<?, ?> cause = assertThatThrownBy(
                () -> openIdConnectService.processAuth(request, context, "main").subscribeAsCompletionStage().join()
        ).hasCauseInstanceOf(ServiceException.class).cause();

        cause.extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.GENERIC_AUTH_FAILURE.getCode());

        cause.extracting(Throwable::getMessage)
                .isEqualTo("Redirect URL doesn't match the client base URL");
    }

    @Test
    void processAuthCodeToken() {
        AuthRequestBO request = AuthRequestBO.builder()
                .token("token")
                .build();
        RequestContextBO context = RequestContextBO.builder()
                .userAgent("test")
                .build();

        openIdConnectService.processAuthCodeToken(request, context);

        Mockito.verify(exchangeService).exchange(request, "authorizationCode", "oidc", context);
    }

    @Test
    void processRefreshToken() {
        AuthRequestBO request = AuthRequestBO.builder()
                .token("token")
                .build();
        RequestContextBO context = RequestContextBO.builder()
                .userAgent("test")
                .build();

        openIdConnectService.processRefreshToken(request, context);

        Mockito.verify(exchangeService).exchange(request, "refresh", "accessToken", context);
    }

    @Test
    void processAuthPkce() {
        OpenIdConnectRequest pkceRequest = request
                .withCodeChallenge("random-code-challenge")
                .withCodeChallengeMethod("S256");

        RequestContextBO context = RequestContextBO.builder().build();

        AuthRequestBO expectedRequest = AuthRequestBO.builder()
                .domain(client.getDomain())
                .identifier(pkceRequest.getIdentifier())
                .password(pkceRequest.getPassword())
                .externalSessionId(pkceRequest.getExternalSessionId())
                .extraParameters(PkceParameters.forAuthCode(pkceRequest.getCodeChallenge(), pkceRequest.getCodeChallengeMethod()))
                .build();
        AuthResponseBO expectedResponse = AuthResponseBO.builder()
                .token("auth code")
                .build();

        Mockito.when(clientsService.getById(1, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));

        Mockito.when(exchangeService.exchange(expectedRequest, "basic", "authorizationCode", context.withClientId("1")))
                .thenReturn(Uni.createFrom().item(expectedResponse));

        assertThat(openIdConnectService.processAuth(pkceRequest, context, "main").subscribeAsCompletionStage().join())
                .isEqualTo(expectedResponse);
    }
    @Test
    void processAuthPkceMissingCodeChallenge() {
        OpenIdConnectRequest invalidRequest = request
                .withCodeChallengeMethod("S256");

        Mockito.when(clientsService.getById(1, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));

        AbstractThrowableAssert<?, ?> cause = assertThatThrownBy(
                () -> openIdConnectService.processAuth(invalidRequest, context, "main").subscribeAsCompletionStage().join()
        ).hasCauseInstanceOf(ServiceException.class).cause();

        cause.extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.GENERIC_AUTH_FAILURE.getCode());

        cause.extracting(Throwable::getMessage)
                .isEqualTo("Code challenge missing");
    }

    @Test
    void processAuthPkceMissingCodeChallengeMethod() {
        OpenIdConnectRequest invalidRequest = request
                .withCodeChallenge("random-code-challenge");

        Mockito.when(clientsService.getById(1, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));

        AbstractThrowableAssert<?, ?> cause = assertThatThrownBy(
                () -> openIdConnectService.processAuth(invalidRequest, context, "main").subscribeAsCompletionStage().join()
        ).hasCauseInstanceOf(ServiceException.class).cause();

        cause.extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.GENERIC_AUTH_FAILURE.getCode());

        cause.extracting(Throwable::getMessage)
                .isEqualTo("Code challenge method must be S256 (SHA-256)");
    }

    @Test
    void processAuthPkceInvalidCodeChallengeMethod() {
        OpenIdConnectRequest invalidRequest = request
                .withCodeChallenge("random-code-challenge")
                .withCodeChallengeMethod("invalid");

        Mockito.when(clientsService.getById(1, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));

        AbstractThrowableAssert<?, ?> cause = assertThatThrownBy(
                () -> openIdConnectService.processAuth(invalidRequest, context, "main").subscribeAsCompletionStage().join()
        ).hasCauseInstanceOf(ServiceException.class).cause();

        cause.extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.GENERIC_AUTH_FAILURE.getCode());

        cause.extracting(Throwable::getMessage)
                .isEqualTo("Code challenge method must be S256 (SHA-256)");
    }
}