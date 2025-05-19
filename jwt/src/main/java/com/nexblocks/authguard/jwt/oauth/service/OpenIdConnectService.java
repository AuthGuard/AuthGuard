package com.nexblocks.authguard.jwt.oauth.service;

import com.google.inject.Inject;
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
import com.nexblocks.authguard.service.random.CryptographicRandom;
import com.nexblocks.authguard.service.util.ID;
import io.smallrye.mutiny.Uni;
import io.vavr.control.Try;
import okhttp3.HttpUrl;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

public class OpenIdConnectService {
    private static final String BASIC_TOKEN_TYPE = "basic";
    private static final String AUTH_CODE_TOKEN_TYPE = "authorizationCode";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String OIDC_TOKEN_TYPE = "oidc";
    private static final String ACCESS_TOKEN_TYPE = "accessToken";
    private static final int REQUEST_TOKEN_SIZE = 64;
    private static final Duration TOKEN_TTL = Duration.ofDays(1);

    private final ClientsService clientsService;
    private final ExchangeService exchangeService;
    private final TrackingSessionsService trackingSessionsService;
    private final AccountTokensRepository accountTokensRepository;
    private final CryptographicRandom cryptographicRandom;

    @Inject
    public OpenIdConnectService(ClientsService clientsService, ExchangeService exchangeService,
                                TrackingSessionsService trackingSessionsService,
                                AccountTokensRepository accountTokensRepository) {
        this.clientsService = clientsService;
        this.exchangeService = exchangeService;
        this.trackingSessionsService = trackingSessionsService;
        this.accountTokensRepository = accountTokensRepository;
        this.cryptographicRandom = new CryptographicRandom();
    }

    public CompletableFuture<AccountTokenDO> createRequestToken(final RequestContextBO requestContext,
                                                                final OpenIdConnectRequest request,
                                                                final String domain) {
        String token = cryptographicRandom.base64Url(REQUEST_TOKEN_SIZE);
        Map<String, String> parameters = new TreeMap<>();

        parameters.put(OAuthConst.Params.ResponseType, request.getResponseType());
        parameters.put(OAuthConst.Params.RedirectUri, request.getRedirectUri());
        parameters.put(OAuthConst.Params.State, request.getState());
        parameters.put(OAuthConst.Params.Scope, String.join(",", request.getScope()));
        parameters.put(OAuthConst.Params.CodeChallengeMethod, request.getCodeChallengeMethod());
        parameters.put(OAuthConst.Params.CodeChallenge, request.getCodeChallenge());

        return trackingSessionsService.startAnonymous(domain)
                .thenCompose(session -> {
                    AccountTokenDO accountToken = AccountTokenDO.builder()
                            .id(ID.generate())
                            .domain(domain)
                            .token(token)
                            .userAgent(requestContext.getUserAgent())
                            .sourceIp(requestContext.getSource())
                            .clientId(request.getClientId())
                            .expiresAt(Instant.now().plus(TOKEN_TTL))
                            .additionalInformation(parameters)
                            .trackingSession(session.getSessionToken())
                            .build();

                    return accountTokensRepository.save(accountToken)
                            .subscribeAsCompletionStage();
                });
    }

    public CompletableFuture<OpenIdConnectRequest> getRequestFromToken(final String token,
                                                                       final RequestContextBO requestContext,
                                                                       final String domain) {
        return accountTokensRepository.getByToken(token)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new ServiceNotFoundException(ErrorCode.INVALID_TOKEN,
                                "invalid_token"));
                    }

                    AccountTokenDO accountToken = opt.get();

                    if (!Objects.equals(accountToken.getDomain(), domain)) {
                        return Uni.createFrom().failure(new ServiceNotFoundException(ErrorCode.INVALID_TOKEN,
                                "invalid_token"));
                    }

                    if (!Objects.equals(accountToken.getUserAgent(), requestContext.getUserAgent())) {
                        return Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                                "invalid_user_agent"));
                    }

                    Map<String, String> parameters = accountToken.getAdditionalInformation();

                    ImmutableOpenIdConnectRequest request = ImmutableOpenIdConnectRequest.builder()
                            .clientId(accountToken.getClientId())
                            .responseType(parameters.get(OAuthConst.Params.ResponseType))
                            .redirectUri(parameters.get(OAuthConst.Params.RedirectUri))
                            .state(parameters.get(OAuthConst.Params.State))
                            .scope(List.of(parameters.get(OAuthConst.Params.Scope).split(",")))
                            .codeChallengeMethod(parameters.get(OAuthConst.Params.CodeChallengeMethod))
                            .codeChallenge(parameters.get(OAuthConst.Params.CodeChallenge))
                            .build();

                    // TODO what else should we check?

                    return Uni.createFrom().item(request);
                })
                .map(OpenIdConnectRequest.class::cast)
                .subscribeAsCompletionStage();
    }

    public CompletableFuture<AuthResponseBO> processAuth(final OpenIdConnectRequest request,
                                                         final RequestContextBO requestContext,
                                                         final String domain) {
        if (!Objects.equals(request.getResponseType(), OAuthConst.ResponseTypes.Code)) {
            return CompletableFuture.failedFuture(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Invalid response type"));
        }

        long parsedId;

        try {
            parsedId = Long.parseLong(request.getClientId());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new ServiceAuthorizationException(ErrorCode.APP_DOES_NOT_EXIST,
                    "Invalid client ID"));
        }

        return clientsService.getById(parsedId, domain)
                .thenCompose(opt -> opt
                        .map(CompletableFuture::completedFuture)
                        .orElseGet(() -> CompletableFuture.failedFuture(new ServiceAuthorizationException(ErrorCode.APP_DOES_NOT_EXIST, "Client does not exist"))))
                .thenCompose(client -> {
                    if (client.getClientType() != Client.ClientType.SSO) {
                        return CompletableFuture.failedFuture(new ServiceException(ErrorCode.CLIENT_NOT_PERMITTED,
                                "Client isn't permitted to perform OIDC requests"));
                    }

                    Try<AuthRequestBO> validatedRequest = verifyClientRequest(client, request.getRedirectUri())
                            .flatMap(ignored -> createRequest(request, client));

                    if (validatedRequest.isFailure()) {
                        return CompletableFuture.failedFuture(validatedRequest.getCause());
                    }

                    return exchangeService.exchange(validatedRequest.get(), BASIC_TOKEN_TYPE, AUTH_CODE_TOKEN_TYPE,
                            requestContext.withClientId(String.valueOf(request.getClientId())));
                });
    }

    public CompletableFuture<AuthResponseBO> processAuthCodeToken(AuthRequestBO request, RequestContextBO requestContext) {
        return exchangeService.exchange(request, AUTH_CODE_TOKEN_TYPE, OIDC_TOKEN_TYPE, requestContext);
    }

    public CompletableFuture<AuthResponseBO> processRefreshToken(AuthRequestBO request, RequestContextBO requestContext) {
        return exchangeService.exchange(request, REFRESH_TOKEN_TYPE, ACCESS_TOKEN_TYPE, requestContext);
    }

    private Try<AuthRequestBO> createRequest(OpenIdConnectRequest request, ClientBO client) {
        boolean isPkce = request.getCodeChallenge() != null
                || request.getCodeChallengeMethod() != null;

        if (isPkce) {
            return verifyPkceRequest(request)
                    .map(ignored -> AuthRequestBO.builder()
                            .domain(client.getDomain())
                            .identifier(request.getIdentifier())
                            .password(request.getPassword())
                            .externalSessionId(request.getExternalSessionId())
                            .extraParameters(PkceParameters.forAuthCode(request.getCodeChallenge(), request.getCodeChallengeMethod()))
                            .build());
        }

        return Try.success(AuthRequestBO.builder()
                .domain(client.getDomain())
                .identifier(request.getIdentifier())
                .password(request.getPassword())
                .externalSessionId(request.getExternalSessionId())
                .build());
    }

    private Try<Boolean> verifyClientRequest(Client client, String redirectUri) {
        if (client.getClientType() != Client.ClientType.SSO) {
            return Try.failure(new ServiceException(ErrorCode.CLIENT_NOT_PERMITTED,
                    "Client isn't permitted to perform OIDC requests"));
        }

        HttpUrl parsedUrl = HttpUrl.parse(redirectUri);

        if (parsedUrl == null) {
            return Try.failure(new ServiceException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Invalid redirect URL"));
        }

        if (!parsedUrl.host().equalsIgnoreCase(client.getBaseUrl())) {
            return Try.failure(new ServiceException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Redirect URL doesn't match the client base URL"));
        }

        return Try.success(true);
    }

    private Try<Boolean> verifyPkceRequest(OpenIdConnectRequest request) {
        if (!Objects.equals(request.getCodeChallengeMethod(), "S256")) {
            return Try.failure(new ServiceException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Code challenge method must be S256 (SHA-256)"));
        }

        if (request.getCodeChallenge() == null) {
            return Try.failure(new ServiceException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Code challenge missing"));
        }

        return Try.success(true);
    }
}
