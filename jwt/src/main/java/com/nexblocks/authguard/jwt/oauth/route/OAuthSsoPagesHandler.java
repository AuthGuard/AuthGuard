package com.nexblocks.authguard.jwt.oauth.route;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.common.RequestContextExtractor;
import com.nexblocks.authguard.api.dto.entities.AuthResponseDTO;
import com.nexblocks.authguard.api.dto.entities.RequestValidationError;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.jwt.exchange.PkceParameters;
import com.nexblocks.authguard.jwt.oauth.config.ImmutableOAuthSsoConfiguration;
import com.nexblocks.authguard.jwt.oauth.config.OAuthSsoConfiguration;
import com.nexblocks.authguard.jwt.oauth.service.ClientUrlUtils;
import com.nexblocks.authguard.jwt.oauth.service.OAuthConst;
import com.nexblocks.authguard.jwt.oauth.service.OpenIdConnectService;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.ClientsService;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.*;
import io.smallrye.mutiny.Uni;
import io.vavr.control.Either;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DependsOnConfiguration("oauthSso")
public class OAuthSsoPagesHandler implements VertxApiHandler {
    private final OpenIdConnectService openIdConnectService;
    private final ApiKeysService apiKeysService;
    private final ClientsService clientsService;
    private final OAuthSsoConfiguration configuration;

    private final String loginPage;
    private final String otpPage;

    @Inject
    public OAuthSsoPagesHandler(@Named("oauthSso") ConfigContext configContext,
                                OpenIdConnectService openIdConnectService,
                                ApiKeysService apiKeysService,
                                ClientsService clientsService) {
        this.configuration = configContext.asConfigBean(ImmutableOAuthSsoConfiguration.class);
        this.openIdConnectService = openIdConnectService;
        this.apiKeysService = apiKeysService;
        this.clientsService = clientsService;

        if (!(configuration.useEmail() || configuration.useUsername() || configuration.usePhoneNumber())) {
            throw new ConfigurationException("SSO must allow at least one identifier type.");
        }

        this.loginPage = replaceParameters(readHtmlPage(configuration.getLoginPage()), configuration);
        this.otpPage = readHtmlPage(configuration.getOtpPage());
    }

    public void register(final Router router) {
        router.get("/oidc/:domain/auth").handler(this::authFlowPage);
        router.get("/oidc/:domain/login").handler(this::loginPage);
        router.post("/oidc/:domain/auth").handler(this::authFlowAuthApi);
        router.post("/oidc/:domain/otp").handler(this::authFlowOtpApi);
        router.post("/oidc/:domain/token").handler(this::authFlowTokenApi);
        router.get("/oidc/:domain/otp").handler(this::otpPage);

        // browser might do a CORS preflight check against /login and /auth
        router.options("/oidc/:domain/login").handler(this::loginPageOptions);
        router.options("/oidc/:domain/auth").handler(this::authFlowAuthApiOptions);
    }

    private void authFlowPage(final RoutingContext context) {
        String domain = context.pathParam("domain");

        if (!configuration.getDomains().contains(domain)) {
            context.response().setStatusCode(404).end();
            return;
        }

        Either<RequestValidationError, ImmutableOpenIdConnectRequest> request =
                OpenIdConnectRequestParser.authRequestFromQueryParams(context, "code");

        if (request.isLeft()) {
            context.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(request.getLeft()));
            return;
        }

        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        openIdConnectService.createRequestToken(requestContext, request.get(), domain)
                .map(token -> {
                    context.response().setStatusCode(302)
                            .putHeader("Location", "/oidc/" + domain + "/login?redirect_uri="
                                    + request.get().getRedirectUri() + "&token=" + token.getToken())
                            .end();
                    return "";
                })
                .onFailure()
                .recoverWithItem(e -> {
                    context.response().setStatusCode(302)
                            .putHeader("Location", "/oidc/login?error=failed")
                            .end();
                    return "";
                })
                .subscribe()
                .asCompletionStage();
    }

    private void loginPage(final RoutingContext context) {
        String domain = context.pathParam("domain");

        if (!configuration.getDomains().contains(domain)) {
            context.response().setStatusCode(404).end();
            return;
        }

        Either<RequestValidationError, ImmutableOpenIdConnectRequest> request =
                OpenIdConnectRequestParser.loginPageRequestFromQueryParams(context);

        if (request.isLeft()) {
            context.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(request.getLeft()));
        } else {
            context.response().putHeader("Content-Type", "text/html").end(loginPage);
        }
    }

    private void loginPageOptions(final RoutingContext context) {
        String domain = context.pathParam("domain");

        if (!configuration.getDomains().contains(domain)) {
            context.response().setStatusCode(404).end();
            return;
        }

        Either<RequestValidationError, ImmutableOpenIdConnectRequest> request =
                OpenIdConnectRequestParser.loginPageRequestFromQueryParams(context);

        if (request.isLeft()) {
            context.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(request.getLeft()));
        } else {
            String origin = ClientUrlUtils.getStandardBaseUrl(request.get().getRedirectUri());

            context.response().setStatusCode(204)
                    .putHeader("Access-Control-Allow-Origin", origin)
                    .putHeader("Access-Control-Allow-Methods", "POST,OPTIONS")
                    .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
                    .putHeader("Access-Control-Expose-Headers", "Location")
                    .end();
        }
    }

    private void authFlowAuthApi(final RoutingContext context) {
        String domain = context.pathParam("domain");
        OpenIdConnectRequest request = Json.decodeValue(context.body().asString(), ImmutableOpenIdConnectRequest.class);
        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        openIdConnectService.getRequestFromToken(request.getRequestToken(), requestContext, domain)
                .flatMap(originalRequest -> {
                    OpenIdConnectRequest realRequest = ImmutableOpenIdConnectRequest.builder()
                            .from(originalRequest)
                            .identifier(request.getIdentifier())
                            .password(request.getPassword())
                            .build();

                    boolean useOtp = configuration.useMultiFactorAuthentication() != null
                            && configuration.useMultiFactorAuthentication().useOtp();

                    Uni<AuthResponseBO> processUnit = useOtp ?
                            openIdConnectService.processAuthBasicToOtpFlow(realRequest, requestContext, domain) :
                            openIdConnectService.processAuthBasicToCodeFlow(realRequest, requestContext, domain);

                    return processUnit
                            .map(response -> {
                                if (useOtp) {
                                    AuthResponseDTO dto = AuthResponseDTO.builder()
                                            .token(response.getToken().toString()) // this is actually the ID of the OTP
                                            .type("otp")
                                            .validFor(response.getValidFor())
                                            .trackingSession(response.getTrackingSession())
                                            .build();

                                    return context.response()
                                            .setStatusCode(200)
                                            .end(Json.encode(dto));
                                }

                                String origin = response.getClient().getBaseUrl();

                                String location = realRequest.getRedirectUri() + "?code=" +
                                        URLEncoder.encode(response.getToken().toString(), StandardCharsets.UTF_8)
                                        + "&state=" +
                                        URLEncoder.encode(realRequest.getState(), StandardCharsets.UTF_8);

                                context.response().setStatusCode(302)
                                        .putHeader("Location", location)
                                        .putHeader("Access-Control-Allow-Origin", origin)
                                        .putHeader("Access-Control-Expose-Headers", "Location");

                                return context.end();
                            });
                })
                .subscribe()
                .with(ignored -> {
                    context.end();
                }, e -> {
                    String location;
                    Throwable effectiveException = e instanceof CompletionException ? e.getCause() : e;

                    if (effectiveException instanceof ServiceAuthorizationException ex) {
                        String error = Objects.equals(ex.getErrorCode(), ErrorCode.GENERIC_AUTH_FAILURE.getCode()) ?
                                OAuthConst.ErrorsMessages.UnsupportedResponseType :
                                OAuthConst.ErrorsMessages.UnauthorizedClient;

                        location = request.getRedirectUri() + "?error=" + error;
                    } else if (effectiveException instanceof ServiceException ex) {
                        location = request.getRedirectUri() + "?error=" + ex.getMessage();
                    } else {
                        location = request.getRedirectUri() + "?error=unknown_error";
                    }

                    context.response().setStatusCode(302)
                            .putHeader("Location", location)
                            .end();
                });
    }

    private void authFlowOtpApi(final RoutingContext context) {
        String domain = context.pathParam("domain");
        OpenIdConnectRequest request = Json.decodeValue(context.body().asString(), ImmutableOpenIdConnectRequest.class);
        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        // TODO after getting the request, use it with the OTP to issue a code
        openIdConnectService.getRequestFromToken(request.getRequestToken(), requestContext, domain)
                .flatMap(originalRequest -> {
                    OpenIdConnectRequest realRequest = ImmutableOpenIdConnectRequest.builder()
                            .from(originalRequest)
                            .identifier(request.getIdentifier())
                            .password(request.getPassword())
                            .trackingSession(request.getTrackingSession())
                            .build();

                    return openIdConnectService.processAuthOtpToCodeFlow(realRequest, requestContext, domain)
                            .map(response -> {
                                String origin = response.getClient().getBaseUrl();

                                String location = realRequest.getRedirectUri() + "?code=" +
                                        URLEncoder.encode(response.getToken().toString(), StandardCharsets.UTF_8)
                                        + "&state=" +
                                        URLEncoder.encode(realRequest.getState(), StandardCharsets.UTF_8);

                                context.response().setStatusCode(302)
                                        .putHeader("Location", location)
                                        .putHeader("Access-Control-Allow-Origin", origin)
                                        .putHeader("Access-Control-Expose-Headers", "Location");

                                return context;
                            });
                })
                .subscribe()
                .with(ignored -> {
                    context.end();
                }, e -> {
                    String location;
                    Throwable effectiveException = e instanceof CompletionException ? e.getCause() : e;

                    if (effectiveException instanceof ServiceAuthorizationException ex) {
                        String error = Objects.equals(ex.getErrorCode(), ErrorCode.GENERIC_AUTH_FAILURE.getCode()) ?
                                OAuthConst.ErrorsMessages.UnsupportedResponseType :
                                OAuthConst.ErrorsMessages.UnauthorizedClient;

                        location = request.getRedirectUri() + "?error=" + error;
                    } else if (effectiveException instanceof ServiceException ex) {
                        location = request.getRedirectUri() + "?error=" + ex.getMessage();
                    } else {
                        location = request.getRedirectUri() + "?error=unknown_error";
                    }

                    context.response().setStatusCode(302)
                            .putHeader("Location", location)
                            .end();
                });
    }

    private void authFlowAuthApiOptions(final RoutingContext context) {
        String domain = context.pathParam("domain");
        Either<RequestValidationError, ImmutableOpenIdConnectRequest> request =
                OpenIdConnectRequestParser.authRequestFromQueryParams(context, "code");

        if (request.isLeft()) {
            context.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(request.getLeft()));
            return;
        }

        OpenIdConnectRequest openIdConnectRequest = request.get();
        Long clientId = Long.parseLong(openIdConnectRequest.getClientId());
        clientsService.getById(clientId, domain)
                .subscribe()
                .with(opt -> {
                    if (opt.isEmpty()) {
                        context.response().setStatusCode(400)
                                .end();
                        return;
                    }

                    ClientBO client = opt.get();
                    String origin = client.getBaseUrl();

                    context.response().setStatusCode(204)
                            .putHeader("Access-Control-Allow-Origin", origin)
                            .putHeader("Access-Control-Allow-Methods", "POST,OPTIONS")
                            .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
                            .putHeader("Access-Control-Expose-Headers", "Location")
                            .end();
                });
    }

    private void authFlowTokenApi(final RoutingContext context) {
        String domain = context.pathParam("domain");

        if (!configuration.getDomains().contains(domain)) {
            context.response().setStatusCode(404).end();
            return;
        }

        String grantType = context.request().getFormAttribute(OAuthConst.Params.GrantType);

        if (grantType == null) {
            context.response().setStatusCode(400).putHeader("Content-Type", "application/json")
                    .end(Json.encode(new OpenIdConnectError(OAuthConst.ErrorsMessages.InvalidGrant, "Invalid grant type")));
            return;
        }

        Either<OpenIdConnectError, String[]> clientInfoOrError = extractClientInfo(context);

        if (clientInfoOrError.isLeft()) {
            context.response().setStatusCode(400).putHeader("Content-Type", "application/json")
                    .end(Json.encode(clientInfoOrError.getLeft()));
            return;
        }

        String clientId = clientInfoOrError.get()[0];
        String clientSecret = clientInfoOrError.get()[1];

        String codeVerifier = context.request().getFormAttribute(OAuthConst.Params.CodeVerifier);

        Uni<ClientBO> verifiedClient;

        if (clientSecret != null && codeVerifier == null) {
            verifiedClient = verifyNonPkceTokenFlow(clientId, clientSecret, domain);
        } else if (codeVerifier != null && clientSecret == null) {
            verifiedClient = verifyPkceTokenFlow(clientId, domain);
        } else {
            context.response().setStatusCode(400).putHeader("Content-Type", "application/json")
                    .end(Json.encode(new OpenIdConnectError(OAuthConst.ErrorsMessages.InvalidRequest,
                            "Either client_secret or code_verifier must be set. Not both or neither.")));
            return;
        }

        verifiedClient.flatMap(client -> {
                    if (Objects.equals(grantType, OAuthConst.GrantTypes.AuthorizationCode)) {
                        return handleAuthorizationCode(context, client, codeVerifier);
                    } else if (Objects.equals(grantType, OAuthConst.GrantTypes.RefreshToken)) {
                        return handleRefreshToken(context, client);
                    } else {
                        return Uni.createFrom().failure(new ServiceException("invalid_grant", "Invalid grant type"));
                    }
                })
                .onFailure()
                .recoverWithItem(e -> {
                    Throwable cause = e instanceof CompletionException ? e.getCause() : e;

                    if (cause instanceof ServiceAuthorizationException) {
                        context.response().setStatusCode(401).putHeader("Content-Type", "application/json")
                                .end(Json.encode(new OpenIdConnectError(((ServiceAuthorizationException) cause).getErrorCode(), cause.getMessage())));
                    } else if (cause instanceof ServiceException) {
                        context.response().setStatusCode(400).putHeader("Content-Type", "application/json")
                                .end(Json.encode(new OpenIdConnectError(((ServiceException) cause).getErrorCode(), cause.getMessage())));
                    } else {
                        context.response().setStatusCode(500).putHeader("Content-Type", "application/json")
                                .end(Json.encode(new OpenIdConnectError(ErrorCode.GENERIC_AUTH_FAILURE.getCode(), "Unknown error")));
                    }

                    return null;
                })
                .onItem()
                .invoke(result -> {
                    if (result != null) {
                        context.response().putHeader("Content-Type", "application/json")
                                .end(Json.encode(result));
                    }
                })
                .subscribe()
                .asCompletionStage();
    }

    private Either<OpenIdConnectError, String[]> extractClientInfo(RoutingContext context) {
        String clientId = context.request().getFormAttribute(OAuthConst.Params.ClientId);
        String clientSecret = context.request().getFormAttribute(OAuthConst.Params.ClientSecret);

        String header = context.request().getHeader("Authorization");

        // if we have no clientId from the forms, but we have an Authorization
        // header then we should try to get it from there
        if (header != null && clientId == null) {
            String[] parts = header.split(" ");
            if (parts.length != 2) {
                return Either.left(new OpenIdConnectError(OAuthConst.ErrorsMessages.InvalidRequest,
                        "invalid Authorization header"));
            }

            String[] decoded = new String(Base64.getDecoder().decode(parts[1])).split(":");

            if (decoded.length != 2) {
                return Either.left(new OpenIdConnectError(OAuthConst.ErrorsMessages.InvalidRequest,
                        "invalid Authorization header"));
            }

            clientId =  decoded[0];
            clientSecret = decoded[1];
        }

        String decodedSecret = URLDecoder.decode(clientSecret, StandardCharsets.UTF_8);
        return Either.right(new String[] { clientId, decodedSecret });
    }

    private Uni<ClientBO> verifyNonPkceTokenFlow(final String clientId, final String clientSecret, final String domain) {
        return apiKeysService.validateClientApiKey(clientSecret, "default")
                .flatMap(client -> {
                    if (client == null
                            || !Objects.equals(Optional.of(client.getId()).map(Objects::toString).orElse(""), clientId)
                            || client.getClientType() != Client.ClientType.SSO
                            || !Objects.equals(client.getDomain(), domain)) {

                        return Uni.createFrom().failure(new ServiceAuthorizationException("invalid_request", "Invalid secret or unauthorized client"));
                    }

                    return Uni.createFrom().item(client);
                });
    }

    private Uni<ClientBO> verifyPkceTokenFlow(String clientId, String domain) {
        return clientsService.getById(Long.parseLong(clientId), domain)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new ServiceAuthorizationException("invalid_request", "Invalid client ID or unauthorized client"));
                    }

                    return Uni.createFrom().item(opt.get());
                });
    }

    private Uni<Object> handleAuthorizationCode(RoutingContext context, Client client, String codeVerifier) {
        String authorizationCode = context.request().getFormAttribute("code");

        if (authorizationCode == null || authorizationCode.isBlank()) {
            return Uni.createFrom().failure(new ServiceAuthorizationException("invalid_request", "Invalid authorization code"));
        }

        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context, client);

        AuthRequestBO.Builder request = AuthRequestBO.builder().token(authorizationCode);

        if (codeVerifier != null) {
            request.extraParameters(PkceParameters.forToken(codeVerifier));
        }

        return openIdConnectService.processAuthCodeToken(request.build(), requestContext)
                .map(authCodeResponse -> {
                    OAuthResponseBO oAuthResponse = (OAuthResponseBO) authCodeResponse.getToken();

                    return new OpenIdConnectResponse(
                            oAuthResponse.getAccessToken(),
                            oAuthResponse.getIdToken(),
                            oAuthResponse.getRefreshToken(),
                            authCodeResponse.getValidFor()
                    );
                });
    }

    private Uni<Object> handleRefreshToken(RoutingContext context, Client client) {
        String refreshToken = context.request().getFormAttribute("refresh_token");

        if (refreshToken == null || refreshToken.isBlank()) {
            return Uni.createFrom().failure(new ServiceException("invalid_request", "Invalid refresh token"));
        }

        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context, client);

        AuthRequestBO request = AuthRequestBO.builder().token(refreshToken).build();

        return openIdConnectService.processRefreshToken(request, requestContext)
                .map(refreshResponse -> new OpenIdConnectResponse(
                        (String) refreshResponse.getToken(),
                        null,
                        (String) refreshResponse.getRefreshToken(),
                        refreshResponse.getValidFor()
                ));
    }

// ... remaining content



    private void otpPage(final RoutingContext context) {
        String domain = context.pathParam("domain");

        if (!configuration.getDomains().contains(domain)) {
            context.response().setStatusCode(404).end();
            return;
        }

        context.response().putHeader("Content-Type", "text/html").end(otpPage);
    }

    private String readHtmlPage(final String path) {
        InputStream stream = resolveFilePath(path);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(" "));
        } catch (IOException e) {
            throw new ConfigurationException("Failed to read file " + path, e);
        }
    }

    private InputStream resolveFilePath(final String path) {
        if (path.startsWith("resources/")) {
            String resourceName = path.substring(path.indexOf("/") + 1);
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);

            if (stream == null) {
                throw new ConfigurationException("Resource file " + path + " resolved to nothing");
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
        String identifierPlaceholder = Stream.of(
                        config.useUsername() ? "username" : null,
                        config.useEmail() ? "email" : null,
                        config.usePhoneNumber() ? "phone number" : null)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));

        return page.replace("${identifierPlaceholder}", StringUtils.capitalize(identifierPlaceholder));
    }
}
