package com.nexblocks.authguard.jwt.oauth.route;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.common.RequestContextExtractor;
import com.nexblocks.authguard.api.common.RestJsonMapper;
import com.nexblocks.authguard.api.dto.entities.RequestValidationError;
import com.nexblocks.authguard.api.routes.ApiRoute;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.jwt.exchange.PkceParameters;
import com.nexblocks.authguard.jwt.oauth.config.ImmutableOAuthSsoConfiguration;
import com.nexblocks.authguard.jwt.oauth.config.OAuthSsoConfiguration;
import com.nexblocks.authguard.jwt.oauth.service.OpenIdConnectService;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.ClientsService;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.*;
import io.javalin.http.Context;
import io.vavr.control.Either;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

@DependsOnConfiguration("oauthSso")
public class OAuthSsoPagesRoute implements ApiRoute {
    private final OpenIdConnectService openIdConnectService;
    private final ApiKeysService apiKeysService;
    private final ClientsService clientsService;
    private final OAuthSsoConfiguration configuration;

    private final String loginPage;
    private final String otpPage;

    @Inject
    public OAuthSsoPagesRoute(@Named("oauthSso") ConfigContext configContext,
                              OpenIdConnectService openIdConnectService,
                              ApiKeysService apiKeysService,
                              ClientsService clientsService) {
        this.configuration = configContext.asConfigBean(ImmutableOAuthSsoConfiguration.class);
        this.openIdConnectService = openIdConnectService;
        this.apiKeysService = apiKeysService;
        this.clientsService = clientsService;

        if (!(configuration.useEmail() || configuration.useUsername() || configuration.usePhoneNumber())) {
            throw new ConfigurationException("SSO must be allowed with at least one identifier type. " +
                    "Set one or more of useUsername, useEmail, or setPhoneNumber to true.");
        }

        this.loginPage = replaceParameters(readHtmlPage(configuration.getLoginPage()), configuration);
        this.otpPage = readHtmlPage(configuration.getOtpPage());
    }

    @Override
    public String getPath() {
        return "oidc";
    }

    @Override
    public void addEndpoints() {
        get("/auth", this::authFlowPage);
        post("/auth", this::authFlowAuthApi);
        post("/token", this::authFlowTokenApi);
        get("/otp", this::otpPage);
    }

    private void authFlowPage(Context context) {
        Either<RequestValidationError, ImmutableOpenIdConnectRequest> request
                = OpenIdConnectRequestParser.authRequestFromQueryParams(context, "code");

        if (request.isLeft()) {
            context.status(400).json(request.getLeft());
        } else {
            context.status(200).html(loginPage);
        }
    }

    private void authFlowAuthApi(Context context) {
        OpenIdConnectRequest request = RestJsonMapper.asClass(context.body(), ImmutableOpenIdConnectRequest.class);
        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        try {
            AuthResponse response = openIdConnectService.processAuth(request, requestContext).join();
            String formedUrl = request.getRedirectUri() + "?code=" + response.getToken()
                    + "&state=" + request.getState();

            context.redirect(formedUrl);
        } catch (ServiceAuthorizationException ex) {
            String error = Objects.equals(ex.getErrorCode(), ErrorCode.GENERIC_AUTH_FAILURE.getCode()) ?
                    "unsupported_response_type" :
                    "unauthorized_client";
            String formedUrl = request.getRedirectUri() + "?error=" + error;

            context.redirect(formedUrl);
        } catch (ServiceException ex) {
            String formedUrl = request.getRedirectUri() + "?error=" + ex.getErrorCode();

            context.redirect(formedUrl);
        }
    }

    private void authFlowTokenApi(Context context) {
        String grantType = context.formParam("grant_type");
        String clientId = context.formParam("client_id");
        String clientSecret = context.formParam("client_secret");
        String codeVerifier = context.formParam("code_verifier");

        if (grantType == null) {
            context.status(400)
                    .json(new OpenIdConnectError("invalid_grant", "Invalid grant type"));
            return;
        }

        CompletableFuture<ClientBO> verifiedClient;

        if (clientSecret != null && codeVerifier == null) {
            verifiedClient = verifyNonPkceTokenFlow(context, clientId, clientSecret);
        } else if (codeVerifier != null && clientSecret == null) {
            verifiedClient = verifyPkceTokenFlow(context, clientId);
        } else {
            context.status(400)
                    .json(new OpenIdConnectError("invalid_request", "Either client_secret or code_verifier must be set. Not both or neither."));
            return;
        }

        verifiedClient.thenCompose(client -> {
                    if (Objects.equals(grantType, "authorization_code")) {
                        return handleAuthorizationCode(context, client, codeVerifier);
                    } else if (Objects.equals(grantType, "refresh_token")) {
                        return handleRefreshToken(context, client);
                    } else {
                        return CompletableFuture.failedFuture(new ServiceException("invalid_grant", "Invalid grant type"));
                    }
                })
                .whenComplete((response, e) -> {
                    if (e != null) {
                        if (ServiceAuthorizationException.class.isAssignableFrom(e.getClass())) {
                            ServiceAuthorizationException serviceException = (ServiceAuthorizationException) e;
                            context.status(401)
                                    .json(new OpenIdConnectError(serviceException.getErrorCode(), serviceException.getMessage()));
                        } else if (ServiceException.class.isAssignableFrom(e.getClass())) {
                            ServiceException serviceException = (ServiceException) e;
                            context.status(400)
                                    .json(new OpenIdConnectError(serviceException.getErrorCode(), serviceException.getMessage()));
                        }
                    }

                    context.json(response);
                });
    }

    private CompletableFuture<ClientBO> verifyNonPkceTokenFlow(Context context, String clientId, String clientSecret) {
        return apiKeysService.validateClientApiKey(clientSecret, "default")
                .thenCompose(client -> {
                    if (client == null
                            || !Objects.equals(Optional.of(client.getId()).map(Objects::toString).orElse(""), clientId)
                            || client.getClientType() != Client.ClientType.SSO) {

                        return CompletableFuture.failedFuture(new ServiceAuthorizationException("invalid_request", "Invalid secret or unauthorized client"));
                    }

                    return CompletableFuture.completedFuture(client);
                });
    }

    private CompletableFuture<ClientBO> verifyPkceTokenFlow(Context context, String clientId) {
        return clientsService.getById(Long.parseLong(clientId))
                .thenCompose(opt -> {
                    if (opt.isEmpty()) {
                        return CompletableFuture.failedFuture(new ServiceAuthorizationException("invalid_request", "Invalid client ID or unauthorized client"));
                    }

                    return CompletableFuture.completedFuture(opt.get());
                });
    }

    private CompletableFuture<OpenIdConnectResponse> handleAuthorizationCode(Context context, Client client, String codeVerifier) {
        String authorizationCode = context.formParam("code");

        if (authorizationCode == null || authorizationCode.isBlank()) {
            return CompletableFuture.failedFuture(new ServiceAuthorizationException("invalid_request", "Invalid authorization code"));
        }

        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context, client);

        AuthRequestBO.Builder request = AuthRequestBO.builder()
                .token(authorizationCode);

        if (codeVerifier != null) {
            request.extraParameters(PkceParameters.forToken(codeVerifier));
        }

        CompletableFuture<OpenIdConnectResponse> response = openIdConnectService.processAuthCodeToken(request.build(), requestContext)
                .thenApply(authCodeResponse -> {
                    OAuthResponseBO oAuthResponse = (OAuthResponseBO) authCodeResponse.getToken();

                    return new OpenIdConnectResponse(
                            oAuthResponse.getAccessToken(),
                            oAuthResponse.getIdToken(),
                            oAuthResponse.getRefreshToken(),
                            authCodeResponse.getValidFor());
                });

        return response;
    }

    private CompletableFuture<OpenIdConnectResponse> handleRefreshToken(Context context, Client client) {
        String refreshToken = context.formParam("refresh_token");

        if (refreshToken == null || refreshToken.isBlank()) {
            CompletableFuture.failedFuture(new ServiceException("invalid_request", "Invalid authorization code"));
        }

        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context, client);

        AuthRequestBO request = AuthRequestBO.builder()
                .token(refreshToken)
                .build();

        CompletableFuture<OpenIdConnectResponse> response = openIdConnectService.processRefreshToken(request, requestContext)
                .thenApply(refreshResponse -> new OpenIdConnectResponse(
                        (String) refreshResponse.getToken(),
                        null,
                        (String) refreshResponse.getRefreshToken(),
                        refreshResponse.getValidFor()));

        return response;
    }

    private void otpPage(Context context) {
        context.status(200).html(otpPage);
    }

    private String readHtmlPage(final String path) {
        InputStream stream = resolveFilePath(path);

        try {
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                BufferedReader bufferedReader = new BufferedReader(reader);

                return bufferedReader.lines().collect(Collectors.joining(" "));
            }
        } catch (Exception e) {
            throw new ConfigurationException("Failed to read file " + path, e);
        }
    }

    private InputStream resolveFilePath(final String path) {
        if (path.startsWith("resources/")) {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            String resourceName = path.substring(path.indexOf("/") + 1);

            InputStream stream = classloader.getResourceAsStream(resourceName);

            if (stream == null) {
                throw new ConfigurationException("Resource file " + path + "resolved to nothing");
            }

            return stream;
        }

        try {
            return new FileInputStream(path);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Failed to resolve file " + path, e);
        }
    }

    private String replaceParameters(final String page, final OAuthSsoConfiguration config) {
        String identifierPlaceholder = Stream.of(config.useUsername() ? "username" : null,
                        config.useEmail() ? "email" : null,
                        config.usePhoneNumber() ? "phone number" : null)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));

        return page.replace("${identifierPlaceholder}", StringUtils.capitalize(identifierPlaceholder));
    }
}
