package com.nexblocks.authguard.jwt.oauth.service;

import com.google.inject.Inject;
import com.nexblocks.authguard.jwt.oauth.route.OpenIdConnectRequest;
import com.nexblocks.authguard.service.ClientsService;
import com.nexblocks.authguard.service.ExchangeService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.util.AsyncUtils;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class OpenIdConnectService {
    private static final String BASIC_TOKEN_TYPE = "basic";
    private static final String AUTH_CODE_TOKEN_TYPE = "authorizationCode";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String OIDC_TOKEN_TYPE = "oidc";
    private static final String ACCESS_TOKEN_TYPE = "accessToken";

    private final ClientsService clientsService;
    private final ExchangeService exchangeService;

    @Inject
    public OpenIdConnectService(ClientsService clientsService, ExchangeService exchangeService) {
        this.clientsService = clientsService;
        this.exchangeService = exchangeService;
    }

    public CompletableFuture<AuthResponseBO> processAuth(OpenIdConnectRequest request, RequestContextBO requestContext) {
        if (!Objects.equals(request.getResponseType(), "code")) {
            throw new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Invalid response type");
        }

        long parsedId;

        try {
            parsedId = Long.parseLong(request.getClientId());
        } catch (Exception e) {
            throw new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Invalid client ID");
        }

        return clientsService.getById(parsedId)
                .thenCompose(AsyncUtils::fromClientOptional)
                .thenCompose(client -> {
                    if (client.getClientType() != Client.ClientType.SSO) {
                        return CompletableFuture.failedFuture(new ServiceAuthorizationException(ErrorCode.CLIENT_NOT_PERMITTED,
                                "Client isn't permitted to perform OIDC requests"));
                    }

                    // TODO validate redirect URI
                    // TODO store state
                    // TODO validate response type

                    AuthRequestBO authRequest = createRequest(request, client);

                    return exchangeService.exchange(authRequest, BASIC_TOKEN_TYPE, AUTH_CODE_TOKEN_TYPE,
                            requestContext.withClientId(String.valueOf(request.getClientId())));
                });
    }

    public CompletableFuture<AuthResponseBO> processAuthCodeToken(AuthRequestBO request, RequestContextBO requestContext) {
        return exchangeService.exchange(request, AUTH_CODE_TOKEN_TYPE, OIDC_TOKEN_TYPE, requestContext);
    }

    public CompletableFuture<AuthResponseBO> processRefreshToken(AuthRequestBO request, RequestContextBO requestContext) {
        return exchangeService.exchange(request, REFRESH_TOKEN_TYPE, ACCESS_TOKEN_TYPE, requestContext);
    }

    private AuthRequestBO createRequest(OpenIdConnectRequest request, ClientBO client) {
        return AuthRequestBO.builder()
                .domain(client.getDomain())
                .identifier(request.getIdentifier())
                .password(request.getPassword())
                .externalSessionId(request.getExternalSessionId())
                .build();
    }
}
