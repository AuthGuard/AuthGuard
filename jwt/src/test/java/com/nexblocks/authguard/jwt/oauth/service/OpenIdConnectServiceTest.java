package com.nexblocks.authguard.jwt.oauth.service;

import com.nexblocks.authguard.jwt.oauth.route.ImmutableOpenIdConnectRequest;
import com.nexblocks.authguard.jwt.oauth.route.OpenIdConnectRequest;
import com.nexblocks.authguard.service.ClientsService;
import com.nexblocks.authguard.service.ExchangeService;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.*;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenIdConnectServiceTest {
    private ExchangeService exchangeService;
    private ClientsService clientsService;
    private OpenIdConnectService openIdConnectService;

    @BeforeEach
    void setup() {
        exchangeService = Mockito.mock(ExchangeService.class);
        clientsService = Mockito.mock(ClientsService.class);
        openIdConnectService = new OpenIdConnectService(clientsService, exchangeService);
    }

    @Test
    void processAuth() {
        ClientBO client = ClientBO.builder()
                .domain("test")
                .clientType(Client.ClientType.SSO)
                .baseUrl("test-domain.com")
                .build();

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

        Mockito.when(clientsService.getById(1))
                        .thenReturn(CompletableFuture.completedFuture(Optional.of(client)));

        Mockito.when(exchangeService.exchange(expectedRequest, "basic", "authorizationCode", context))
                .thenReturn(CompletableFuture.completedFuture(expectedResponse));

        assertThat(openIdConnectService.processAuth(request, context).join())
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

        Mockito.when(clientsService.getById(1))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> openIdConnectService.processAuth(request, context).join())
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

        Mockito.when(clientsService.getById(1))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(client)));

        assertThatThrownBy(() -> openIdConnectService.processAuth(request, context).join())
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

        Mockito.when(clientsService.getById(1))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(client)));

        AbstractThrowableAssert<?, ?> cause = assertThatThrownBy(() -> openIdConnectService.processAuth(request, context).join())
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

        Mockito.when(clientsService.getById(1))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(client)));

        AbstractThrowableAssert<?, ?> cause = assertThatThrownBy(() -> openIdConnectService.processAuth(request, context).join())
                .hasCauseInstanceOf(ServiceException.class)
                .cause();

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
}