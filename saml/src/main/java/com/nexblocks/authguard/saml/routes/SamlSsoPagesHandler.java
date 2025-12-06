package com.nexblocks.authguard.saml.routes;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.common.RequestContextExtractor;
import com.nexblocks.authguard.api.dto.entities.AuthResponseDTO;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.api.dto.entities.RequestValidationError;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.RoutesConstants;
import com.nexblocks.authguard.api.routes.StatelessCsrf;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.saml.*;
import com.nexblocks.authguard.saml.config.ImmutableSamlConfiguration;
import com.nexblocks.authguard.saml.config.SamlConfiguration;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import io.smallrye.mutiny.Uni;
import io.vavr.control.Either;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DependsOnConfiguration("saml")
public class SamlSsoPagesHandler implements VertxApiHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SamlSsoPagesHandler.class);

    private final SamlService samlService;
    private final StatelessCsrf statelessCsrf;
    private final SamlRequestParser requestParser;
    private final SamlConfiguration configuration;

    private final String loginPage;

    @Inject
    public SamlSsoPagesHandler(final SamlService samlService,
                               final @Named("saml") ConfigContext configContext) {
        this.configuration = configContext.asConfigBean(ImmutableSamlConfiguration.class);
        this.samlService = samlService;

        if (!(configuration.useEmail() || configuration.useUsername() || configuration.usePhoneNumber())) {
            throw new ConfigurationException("SSO must allow at least one identifier type.");
        }

        if (configuration.getCsrfTokenKey() == null || configuration.getCsrfTokenKey().isBlank()) {
            throw new ConfigurationException("SSO must include a CSRF token key.");
        }

        this.statelessCsrf = new StatelessCsrf(configuration.getCsrfTokenKey());
        this.requestParser = new SamlRequestParser(this.configuration);
        this.loginPage = replaceParameters(readHtmlPage(configuration.getLoginPage()), configuration);
    }

    @Override
    public void register(final Router router) {
        router.get("/saml/:domain/sso").handler(this::samlAuthPage);
        router.post("/saml/:domain/sso").handler(this::samlAuthPagePostBinding);
        router.get("/saml/:domain/login").handler(this::loginPage);
        router.post("/saml/:domain/session").handler(this::sessionApi);
        router.post("/saml/:domain/auth").handler(this::authFlowAuthApi);
        router.post("/saml/:domain/otp").handler(this::authFlowOtpApi);
    }

    private void samlAuthPage(final RoutingContext context) {
        String domain = context.pathParam("domain");

        if (!configuration.getDomains().contains(domain)) {
            context.response().setStatusCode(404).end();
            return;
        }

        String samlRequest = context.queryParam("SAMLRequest").stream().findFirst().orElse(null);
        String relayState = context.queryParam("RelayState").stream().findFirst().orElse(null);

        Either<SamlErrorResponse, SamlAuthnRequest> request =
                requestParser.parseAuthnRequest(SamlRequestParser.Binding.REDIRECT, samlRequest, relayState);

        if (request.isLeft()) {
            SamlErrorResponse requestError = request.getLeft();

            if (requestError.isCanReturnToSp()) {
                String response = SamlResponseMarshaller.toBase64XmlString(requestError.getResponse());
                sendAutoSubmitForm(context, requestError.getAcsUrl(),
                        requestError.getAcsUrl(), response, relayState);

                return;
            }

            context.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(request.getLeft()));
            return;
        }

        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        samlService.createRequestToken(requestContext, request.get(), domain)
                .map(token -> {
                    context.response().setStatusCode(302)
                            .putHeader("Location", "/saml/" + domain + "/login?token=" + token.getToken())
                            .end();
                    return "";
                })
                .onFailure()
                .recoverWithItem(e -> {
                    Response errorResponse = SamlErrorResponseProvider.authnFailed(
                            configuration.getIssuer(),
                            request.get().getAcsUrl(),
                            request.get().getRequestId(),
                            "Failed to start authentication process");

                    String response = SamlResponseMarshaller.toBase64XmlString(errorResponse);

                    sendAutoSubmitForm(context, request.get().getAcsUrl(),
                            request.get().getAcsUrl(), response, relayState);

                    return "";
                })
                .subscribe()
                .asCompletionStage();
    }

    private void samlAuthPagePostBinding(final RoutingContext context) {
        String domain = context.pathParam("domain");

        if (!configuration.getDomains().contains(domain)) {
            context.response().setStatusCode(404).end();
            return;
        }

        String samlRequest = context.request().getParam("SAMLRequest");
        String relayState = context.request().getParam("RelayState");

        Either<SamlErrorResponse, SamlAuthnRequest> request =
                requestParser.parseAuthnRequest(SamlRequestParser.Binding.POST, samlRequest, relayState);

        if (request.isLeft()) {
            SamlErrorResponse requestError = request.getLeft();

            if (requestError.isCanReturnToSp()) {
                String response = SamlResponseMarshaller.toBase64XmlString(requestError.getResponse());
                sendAutoSubmitForm(context, requestError.getAcsUrl(),
                        requestError.getAcsUrl(), response, relayState);

                return;
            }

            context.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(request.getLeft()));
            return;
        }

        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        samlService.createRequestToken(requestContext, request.get(), domain)
                .map(token -> {
                    context.response().setStatusCode(302)
                            .putHeader("Location", "/saml/" + domain + "/login?token=" + token.getToken())
                            .end();
                    return "";
                })
                .onFailure()
                .recoverWithItem(e -> {
                    Response errorResponse = SamlErrorResponseProvider.authnFailed(
                            configuration.getIssuer(),
                            request.get().getAcsUrl(),
                            request.get().getRequestId(),
                            "Failed to start authentication process");

                    String response = SamlResponseMarshaller.toBase64XmlString(errorResponse);

                    sendAutoSubmitForm(context, request.get().getAcsUrl(),
                            request.get().getAcsUrl(), response, relayState);

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

        String token = context.queryParam(SamlConst.Params.Token)
                .stream().findFirst().orElse(null);

        if (token == null) {
            RequestValidationError error = new RequestValidationError(
                    List.of(new Violation(SamlConst.Params.Token, ViolationType.MISSING_REQUIRED_VALUE)));

            context.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(error));

            return;
        }

        String csrfToken = statelessCsrf.generate(token);
        Cookie cookie = Cookie.cookie(RoutesConstants.RES_CSRF_COOKIE, csrfToken)
                .setSameSite(CookieSameSite.STRICT)
                .setMaxAge(5 * 60);

        context.response()
                .putHeader("Content-Type", "text/html")
                .addCookie(cookie)
                .end(loginPage);
    }

    private void sessionApi(final RoutingContext context) {
        String domain = context.pathParam("domain");
        SamlLoginRequest request = Json.decodeValue(context.body().asString(), ImmutableSamlLoginRequest.class);
        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        if (isRequestCsrfTokenValid(context, request.getRequestToken())) {
            com.nexblocks.authguard.api.dto.entities.Error csrfError =
                    new com.nexblocks.authguard.api.dto.entities.Error(ErrorCode.FAILED_CSRF_CHECK.getCode(),
                            "Missing or invalid CSRF header");

            context.response()
                    .setStatusCode(400)
                    .end(Json.encode(csrfError));

            return;
        }

        Cookie sessionToken = context.request().cookies(RoutesConstants.AUTH_SESSION_TOKEN_COOKIE)
                .stream().findFirst().orElse(null);

        if (sessionToken == null) {
            context.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(new Error(ErrorCode.INVALID_TOKEN.getCode(), "No session code was provided")));

            return;
        }

        samlService.getRequestFromToken(request.getRequestToken(), requestContext, domain)
                .flatMap(originalRequest -> samlService.processSessionToSamlResponse(originalRequest, sessionToken.getValue(), request, requestContext, domain)
                        .map(response -> {
                            String origin = response.getClient().getBaseUrl();
                            String location = buildAcsUrl(originalRequest, response);
                            String html = PostBindingPage.render(location, (String) response.getToken(), originalRequest.getRelayState());

                            return context.response()
                                    .putHeader("Access-Control-Allow-Origin", origin)
                                    .putHeader("Content-Type", "text/html; charset=utf-8")
                                    .putHeader("Cache-Control", "no-store")
//                                    .addCookie(Cookie.cookie("AG_ST", response.getTrackingSession()))
                                    .end(html);
                        }))
                .subscribe()
                .with(ignored -> {
                    if (!context.response().ended()) {
                        context.end();
                    }
                }, e -> {
                    Throwable effectiveException = e instanceof CompletionException ? e.getCause() : e;

                    if (effectiveException instanceof ServiceException ex) {
                        Error error = new Error(ex.getErrorCode(), ex.getMessage());
                        context.response().setStatusCode(400)
                                .putHeader("Content-Type", "application/json")
                                .end(Json.encode(error));
                    } else {
                        LOG.error("SAML login failed with an unexpected error", e);
                        context.response().setStatusCode(500)
                                .end();
                    }
                });
    }

    private void authFlowAuthApi(final RoutingContext context) {
        String domain = context.pathParam("domain");
        SamlLoginRequest request = Json.decodeValue(context.body().asString(), ImmutableSamlLoginRequest.class);
        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        if (isRequestCsrfTokenValid(context, request.getRequestToken())) {
            com.nexblocks.authguard.api.dto.entities.Error csrfError =
                    new com.nexblocks.authguard.api.dto.entities.Error(ErrorCode.FAILED_CSRF_CHECK.getCode(),
                            "Missing or invalid CSRF header");

            context.response()
                    .setStatusCode(400)
                    .end(Json.encode(csrfError));

            return;
        }

        samlService.getRequestFromToken(request.getRequestToken(), requestContext, domain)
                .flatMap(originalRequest -> {
                    boolean useOtp = configuration.useMultiFactorAuthentication() != null
                            && configuration.useMultiFactorAuthentication().useOtp();

                    Uni<AuthResponseBO> processUnit = useOtp ?
                            samlService.processAuthBasicToOtp(originalRequest, request, requestContext, domain) :
                            samlService.processAuthBasicToSamlResponse(originalRequest, request, requestContext, domain);

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

                                String html = PostBindingPage.render(
                                        originalRequest.getAcsUrl(),
                                        response.getToken().toString(),
                                        originalRequest.getRelayState()
                                );

                                return context.response()
                                        .putHeader("Access-Control-Allow-Origin", origin)
                                        .putHeader("Content-Type", "text/html; charset=utf-8")
                                        .putHeader("Cache-Control", "no-store")
                                        .addCookie(Cookie.cookie("AG_ST", response.getTrackingSession()))
                                        // TODO add expiry options
                                        .addCookie(Cookie.cookie(RoutesConstants.AUTH_SESSION_TOKEN_COOKIE,
                                                response.getAuthSessionToken()))
                                        .end(html);
                            });
                })
                .subscribe()
                .with(ignored -> {
                    if (!context.response().ended()) {
                        context.end();
                    }
                }, e -> {
                    Throwable effectiveException = e instanceof CompletionException ? e.getCause() : e;

                    if (effectiveException instanceof ServiceException ex) {
                        Error error = new Error(ex.getErrorCode(), ex.getMessage());
                        context.response().setStatusCode(400)
                                .putHeader("Content-Type", "application/json")
                                .end(Json.encode(error));
                    } else {
                        LOG.error("SAML login failed with an unexpected error", e);
                        context.response().setStatusCode(500)
                                .end();
                    }
                });
    }

    private void authFlowOtpApi(final RoutingContext context) {
        String domain = context.pathParam("domain");
        SamlLoginRequest request = Json.decodeValue(context.body().asString(), ImmutableSamlLoginRequest.class);
        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        if (isRequestCsrfTokenValid(context, request.getRequestToken())) {
            com.nexblocks.authguard.api.dto.entities.Error csrfError =
                    new com.nexblocks.authguard.api.dto.entities.Error(ErrorCode.FAILED_CSRF_CHECK.getCode(),
                            "Missing or invalid CSRF header");

            context.response()
                    .setStatusCode(400)
                    .end(Json.encode(csrfError));

            return;
        }

        samlService.getRequestFromToken(request.getRequestToken(), requestContext, domain)
                .flatMap(originalRequest -> {
                    return samlService.processAuthOtpToSamlResponse(originalRequest, request, requestContext, domain)
                            .map(response -> {
                                String origin = response.getClient().getBaseUrl();

                                String location = buildAcsUrl(originalRequest, response);

                                String html = PostBindingPage.render(location, (String) response.getToken(), originalRequest.getRelayState());

                                return context.response()
                                        .putHeader("Access-Control-Allow-Origin", origin)
                                        .putHeader("Content-Type", "text/html; charset=utf-8")
                                        .putHeader("Cache-Control", "no-store")
                                        .addCookie(Cookie.cookie("AG_ST", response.getTrackingSession()))
                                        // TODO add expiry options
                                        .addCookie(Cookie.cookie(RoutesConstants.AUTH_SESSION_TOKEN_COOKIE,
                                                response.getAuthSessionToken()))
                                        .end(html);
                            });
                })
                .subscribe()
                .with(ignored -> {
                    context.end();
                }, e -> {
                    Throwable effectiveException = e instanceof CompletionException ? e.getCause() : e;

                    if (effectiveException instanceof ServiceException ex) {
                        Error error = new Error(ex.getErrorCode(), ex.getMessage());
                        context.response().setStatusCode(400)
                                .putHeader("Content-Type", "application/json")
                                .end(Json.encode(error));
                    } else {
                        LOG.error("SAML login failed with an unexpected error", e);
                        context.response().setStatusCode(500)
                                .end();
                    }
                });
    }

    private void sendAutoSubmitForm(final RoutingContext context,
                                    final String allowOrigin,
                                    final String acsUrl,
                                    final String response,
                                    final String relayState) {
        String html = PostBindingPage.render(acsUrl, response, relayState);

        context.response()
                .putHeader("Content-Type", "text/html; charset=utf-8")
                .putHeader("Cache-Control", "no-store")
                .end(html);
    }

    private String buildAcsUrl(final SamlAuthnRequest originalRequest,
                               final AuthResponseBO response) {
        HttpUrl.Builder builder = HttpUrl.get(originalRequest.getAcsUrl())
                .newBuilder()
                .addQueryParameter("SAMLResponse", response.getToken().toString());

        if (originalRequest.getRelayState() != null) {
            builder.addQueryParameter("RelayState", originalRequest.getRelayState());
        }

        return builder.build().toString();
    }

    // TODO there is a lot of code duplication between this and OAuthSsoPagesHandler
    //  move the common work to the API module
    private boolean isRequestCsrfTokenValid(final RoutingContext context,
                                            final String requestToken) {
        String csrfToken = context.request().getHeader(RoutesConstants.REQ_CSRF_HEADER);

        return csrfToken == null
                || csrfToken.isBlank()
                || !statelessCsrf.isValid(csrfToken, requestToken);
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

    private String replaceParameters(final String page, final SamlConfiguration config) {
        String identifierPlaceholder = Stream.of(
                        config.useUsername() ? "username" : null,
                        config.useEmail() ? "email" : null,
                        config.usePhoneNumber() ? "phone number" : null)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));

        return page.replace("${identifierPlaceholder}", StringUtils.capitalize(identifierPlaceholder));
    }
}
