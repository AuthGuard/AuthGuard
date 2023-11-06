package com.nexblocks.authguard.jwt.oauth.route;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.common.RequestContextExtractor;
import com.nexblocks.authguard.api.common.RestJsonMapper;
import com.nexblocks.authguard.api.dto.entities.RequestValidationError;
import com.nexblocks.authguard.api.routes.ApiRoute;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.jwt.oauth.config.ImmutableOAuthSsoConfiguration;
import com.nexblocks.authguard.jwt.oauth.config.OAuthSsoConfiguration;
import com.nexblocks.authguard.jwt.oauth.service.OAuthService;
import com.nexblocks.authguard.jwt.oauth.service.OpenIdConnectService;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.*;
import io.javalin.http.Context;
import io.vavr.control.Either;
import io.vavr.control.Try;
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
    private final OAuthSsoConfiguration configuration;

    private final String loginPage;
    private final String otpPage;

    @Inject
    public OAuthSsoPagesRoute(@Named("oauthSso") ConfigContext configContext, OAuthService oAuthService,
                              OpenIdConnectService openIdConnectService, ApiKeysService apiKeysService) {
        this.configuration = configContext.asConfigBean(ImmutableOAuthSsoConfiguration.class);
        this.openIdConnectService = openIdConnectService;
        this.apiKeysService = apiKeysService;

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

        if (grantType == null) {
            context.status(400)
                    .json(new OpenIdConnectError("invalid_grant", "Invalid grant type"));
            return;
        }

        apiKeysService.validateClientApiKey(clientSecret, "default")
                .whenComplete((client, e) -> {
                    if (client == null
                            || !Objects.equals(Optional.of(client.getId()).map(Objects::toString).orElse(""), clientId)
                            || client.getClientType() != Client.ClientType.SSO) {
                        context.status(401)
                                .json(new OpenIdConnectError("invalid_request", "Invalid secret or unauthorized client"));

                        return;
                    }

                    if (Objects.equals(grantType, "authorization_code")) {
                        handleAuthorizationCode(context, client);
                    } else if (Objects.equals(grantType, "refresh_token")) {
                        handleRefreshToken(context, client);
                    } else {
                        context.status(400)
                                .json(new OpenIdConnectError("invalid_grant", "Invalid grant type"));
                    }
                });
    }

    private void handleAuthorizationCode(Context context, Client client) {
        String authorizationCode = context.formParam("code");

        if (authorizationCode == null || authorizationCode.isBlank()) {
            context.status(400).
                    json(new OpenIdConnectError("invalid_request", "Invalid authorization code"));
            return;
        }

        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context, client);

        AuthRequestBO request = AuthRequestBO.builder()
                .token(authorizationCode)
                .build();

        CompletableFuture<OpenIdConnectResponse> response = openIdConnectService.processAuthCodeToken(request, requestContext)
                .thenApply(authCodeResponse -> {
                    OAuthResponseBO oAuthResponse = (OAuthResponseBO) authCodeResponse.getToken();

                    return new OpenIdConnectResponse(
                            oAuthResponse.getAccessToken(),
                            oAuthResponse.getIdToken(),
                            oAuthResponse.getRefreshToken(),
                            authCodeResponse.getValidFor());
                });

        context.status(200)
                .json(response);
    }

    private void handleRefreshToken(Context context, Client client) {
        String refreshToken = context.formParam("refresh_token");

        if (refreshToken == null || refreshToken.isBlank()) {
            context.status(400).
                    json(new OpenIdConnectError("invalid_request", "Invalid authorization code"));
            return;
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

        context.json(response);
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
