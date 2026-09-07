package com.nexblocks.authguard.jwt.oauth.route;

import com.nexblocks.authguard.api.dto.entities.AuthResponseDTO;
import com.nexblocks.authguard.api.routes.StatelessCsrf;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.jwt.oauth.config.ImmutableMfaConfiguration;
import com.nexblocks.authguard.jwt.oauth.config.ImmutableOAuthSsoConfiguration;
import com.nexblocks.authguard.jwt.oauth.service.OpenIdConnectService;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.ClientsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.ClientBO;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith({VertxExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OAuthSsoPagesHandlerTest {
    private static final String CSRF_KEY = "test-csrf-key";
    private static final String DOMAIN = "test-domain";

    private OpenIdConnectService openIdConnectService;
    private ApiKeysService apiKeysService;
    private ClientsService clientsService;
    private ConfigContext configContext;

    private StatelessCsrf statelessCsrf;

    @BeforeAll
    void setup() {
        openIdConnectService = Mockito.mock(OpenIdConnectService.class);
        apiKeysService = Mockito.mock(ApiKeysService.class);
        clientsService = Mockito.mock(ClientsService.class);
        configContext = Mockito.mock(ConfigContext.class);

        statelessCsrf = new StatelessCsrf(CSRF_KEY);
    }

    @BeforeEach
    void resetMocks() {
        Mockito.reset(openIdConnectService, apiKeysService, clientsService);
    }

    private OAuthSsoPagesHandler getHandler(boolean withOtp) {
        ImmutableOAuthSsoConfiguration.Builder configuration = ImmutableOAuthSsoConfiguration.builder()
                .addDomains(DOMAIN)
                .useUsername(true)
                .useEmail(true)
                .csrfTokenKey(CSRF_KEY)
                .loginPage("resources/test-sso-login.html");

        if (withOtp) {
            configuration.useMultiFactorAuthentication(ImmutableMfaConfiguration.builder()
                    .useOtp(true)
                    .build());
        }

        Mockito.when(configContext.asConfigBean(ImmutableOAuthSsoConfiguration.class))
                .thenReturn(configuration.build());

        return new OAuthSsoPagesHandler(
                configContext,
                openIdConnectService,
                apiKeysService,
                clientsService
        );
    }

    private String extractCsrfToken(final HttpClientResponse response) {
        String setCookieHeader = response.getHeader("set-cookie");

        if (setCookieHeader == null) {
            return null;
        }

        // Cookie format: "CSRF-TOKEN=value; SameSite=Strict; Max-Age=300"
        // We need to extract just the value part
        String[] parts = setCookieHeader.split(";");
        if (parts.length > 0) {
            String firstPart = parts[0].trim();
            if (firstPart.startsWith("CSRF-TOKEN=")) {
                return firstPart.substring("CSRF-TOKEN=".length());
            }
        }

        return null;
    }

    private <T> void withServer(final Vertx vertx, final int port, final Function<HttpServer, Future<T>> testFlow) {
        Router router = Router.router(vertx);
        OAuthSsoPagesHandler handler = getHandler(false);

        handler.register(router);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port)
                .compose(testFlow);
    }

    private <T> void withOtpServer(final Vertx vertx, final int port, final Function<HttpServer, Future<T>> testFlow) {
        Router router = Router.router(vertx);
        OAuthSsoPagesHandler handler = getHandler(true);

        handler.register(router);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port)
                .compose(testFlow);
    }

    private Future<HttpClientResponse> sendGetRequest(final Vertx vertx, final int port, final String requestUri) {
        return vertx.createHttpClient()
                .request(HttpMethod.GET, port, "localhost", requestUri)
                .compose(HttpClientRequest::send);
    }

    private Future<HttpClientResponse> postForm(final Vertx vertx, final int port, final String requestUri, final String formData) {
        return vertx.createHttpClient()
                .request(HttpMethod.POST, port, "localhost", requestUri)
                .compose(req -> {
                    req.putHeader("Content-Type", "application/x-www-form-urlencoded");
                    req.putHeader("Content-Length", String.valueOf(formData.length()));

                    return req.send(formData);
                });
    }

    private Future<HttpClientResponse> postAuthForm(final Vertx vertx, final int port, final String formData) {
        String postEndpoint = "/oidc/test-domain/auth";

        return postForm(vertx, port, postEndpoint, formData);
    }

    private Future<HttpClientResponse> postOtpForm(final Vertx vertx, final int port, final String formData) {
        String postEndpoint = "/oidc/test-domain/otp";

        return postForm(vertx, port, postEndpoint, formData);
    }

    @Test
    void csrfTokenGenerationAndValidation() {
        String requestToken = "test-request-token";
        String csrfToken = statelessCsrf.generate(requestToken);

        assertThat(csrfToken).isNotNull();
        assertThat(statelessCsrf.isValid(csrfToken, requestToken)).isTrue();
        assertThat(statelessCsrf.isValid(csrfToken, "wrong-token")).isFalse();
        assertThat(statelessCsrf.isValid("invalid-csrf", requestToken)).isFalse();
    }

    @Test
    void authFlowPageToLoginPageFlow(final Vertx vertx, final VertxTestContext testContext) {
        String requestToken = "test-request-token";
        AccountTokenDO token = AccountTokenDO.builder().token(requestToken).build();

        Mockito.when(openIdConnectService.createRequestToken(any(), any(), eq(DOMAIN)))
                .thenReturn(Uni.createFrom().item(token));

        int port = 8080;

        withServer(vertx, port, server -> {
            String requestUri = "/oidc/test-domain/auth?" +
                    "client_id=123&" +
                    "redirect_uri=http://localhost/callback&" +
                    "response_type=code&" +
                    "state=state&" +
                    "scope=oidc";
            // call /auth endpoint to get redirect to login page
            return sendGetRequest(vertx, port, requestUri)
                    .compose(response -> {
                        testContext.verify(() -> {
                            assertThat(response.statusCode()).isEqualTo(302);

                            String location = response.getHeader("Location");

                            assertThat(location).contains("/oidc/test-domain/login");
                            assertThat(location).contains("redirect_uri=");
                            assertThat(location).contains("token=" + requestToken);
                        });

                        // follow redirect to login page to get CSRF token
                        String location = response.getHeader("Location");

                        return sendGetRequest(vertx, port, location);
                    })
                    .onComplete(testContext.succeeding(loginResponse -> testContext.verify(() -> {
                        assertThat(loginResponse.statusCode()).isEqualTo(200);
                        assertThat(loginResponse.getHeader("Content-Type")).isEqualTo("text/html");

                        String setCookie = loginResponse.getHeader("set-cookie");

                        assertThat(setCookie).isNotNull();
                        assertThat(setCookie).contains("CSRF-TOKEN=");

                        String csrfToken = extractCsrfToken(loginResponse);
                        assertThat(csrfToken).isNotNull();

                        server.close().onComplete(testContext.succeedingThenComplete());
                    })));
        });
    }

    @Test
    void authFlowPageInvalidDomain(final Vertx vertx, final VertxTestContext testContext) {
        int port = 8081;

        withServer(vertx, port, server -> {
            String requestUri = "/oidc/invalid-domain/auth?" +
                    "client_id=123&" +
                    "redirect_uri=http://localhost/callback&" +
                    "response_type=code&state=state";

            return sendGetRequest(vertx, port, requestUri)
                    .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                        assertThat(response.statusCode()).isEqualTo(404);

                        server.close().onComplete(testContext.succeedingThenComplete());
                    })));
        });
    }

    @Test
    void completeAuthFlowWithValidCredentialsNoOtp(final Vertx vertx, final VertxTestContext testContext) {
        String requestToken = "test-request-token";
        AccountTokenDO token = AccountTokenDO.builder().token(requestToken).build();

        OpenIdConnectRequest originalRequest = ImmutableOpenIdConnectRequest.builder()
                .clientId("123")
                .redirectUri("http://localhost/callback")
                .responseType("code")
                .state("state")
                .build();

        AuthResponseBO authResponse = AuthResponseBO.builder()
                .token("auth-code")
                .client(ClientBO.builder().baseUrl("http://localhost").build())
                .build();

        Mockito.when(openIdConnectService.createRequestToken(any(), any(), eq(DOMAIN)))
                .thenReturn(Uni.createFrom().item(token));

        Mockito.when(openIdConnectService.getRequestFromToken(eq(requestToken), any(), eq(DOMAIN)))
                .thenReturn(Uni.createFrom().item(originalRequest));

        Mockito.when(openIdConnectService.processAuthBasicToCodeFlow(any(), any(), eq(DOMAIN)))
                .thenReturn(Uni.createFrom().item(authResponse));

        int port = 8082;

        withServer(vertx, port, server -> {
            String requestUri = "/oidc/test-domain/login?" +
                    "redirect_uri=http://localhost/callback&" +
                    "token=" + requestToken;

            // get login page with CSRF token
            return sendGetRequest(vertx, port, requestUri)
                    .compose(loginResponse -> {
                        testContext.verify(() -> {
                            assertThat(loginResponse.statusCode()).isEqualTo(200);
                        });

                        String csrfToken = extractCsrfToken(loginResponse);

                        testContext.verify(() -> {
                            assertThat(csrfToken).isNotNull();
                        });

                        // submit login form with CSRF token
                        String formData = "identifier=testuser&password=testpass&requestToken=" + requestToken + "&csrfToken=" + csrfToken;

                        return postAuthForm(vertx, port, formData);
                    })
                    .compose(response -> {
                        testContext.verify(() -> {
                            assertThat(response.statusCode()).isEqualTo(302);

                            String location = response.getHeader("Location");

                            assertThat(location)
                                    .isEqualTo(String.format("http://localhost/callback?code=%s&state=%s",
                                            authResponse.getToken(),
                                            originalRequest.getState()));
                        });
                        return response.body();
                    })
                    .onComplete(testContext.succeeding(body -> testContext.verify(() -> {
                        server.close().onComplete(testContext.succeedingThenComplete());
                    })));
        });
    }

    @Test
    void completeAuthFlowWithValidCredentialsWithOtp(final Vertx vertx, final VertxTestContext testContext) {
        String requestToken = "test-request-token";
        AccountTokenDO token = AccountTokenDO.builder().token(requestToken).build();

        OpenIdConnectRequest originalRequest = ImmutableOpenIdConnectRequest.builder()
                .clientId("123")
                .redirectUri("http://localhost/callback")
                .responseType("code")
                .state("state")
                .build();

        AuthResponseBO otpResponse = AuthResponseBO.builder()
                .token("otp-transaction-id")
                .type("otp")
                .validFor(300L)
                .trackingSession("tracking-session")
                .build();

        Mockito.when(openIdConnectService.createRequestToken(any(), any(), eq(DOMAIN)))
                .thenReturn(Uni.createFrom().item(token));

        Mockito.when(openIdConnectService.getRequestFromToken(eq(requestToken), any(), eq(DOMAIN)))
                .thenReturn(Uni.createFrom().item(originalRequest));

        Mockito.when(openIdConnectService.processAuthBasicToOtpFlow(any(), any(), eq(DOMAIN)))
                .thenReturn(Uni.createFrom().item(otpResponse));

        int port = 8083;

        withOtpServer(vertx, port, server -> {
            String requestUri = "/oidc/test-domain/login?" +
                    "redirect_uri=http://localhost/callback&" +
                    "token=" + requestToken;

            // get login page with CSRF token
            return sendGetRequest(vertx, port, requestUri)
                    .compose(loginResponse -> {
                        String csrfToken = extractCsrfToken(loginResponse);

                        // submit login form with CSRF token
                        String formData = "identifier=testuser&password=testpass&requestToken=" + requestToken + "&csrfToken=" + csrfToken;

                        return postAuthForm(vertx, port, formData);
                    })
                    .compose(response -> {
                        testContext.verify(() -> {
                            assertThat(response.statusCode()).isEqualTo(200);
                        });
                        return response.body();
                    })
                    .onComplete(testContext.succeeding(body -> testContext.verify(() -> {
                        AuthResponseDTO dto = Json.decodeValue(body.toString(), AuthResponseDTO.class);

                        assertThat(dto.getType()).isEqualTo("otp");
                        assertThat(dto.getToken()).isEqualTo(otpResponse.getToken().toString());
                        assertThat(dto.getTrackingSession()).isEqualTo(otpResponse.getTrackingSession());

                        server.close().onComplete(testContext.succeedingThenComplete());
                    })));
        });
    }

    @Test
    void authFlowAuthApiWithInvalidCsrf(final Vertx vertx, final VertxTestContext testContext) {
        String requestToken = "test-request-token";
        String invalidCsrfToken = "invalid-csrf-token";

        String formData = "identifier=testuser&password=testpass&requestToken=" + requestToken + "&csrfToken=" + invalidCsrfToken;

        int port = 8084;

        withServer(vertx, port, server -> {
            return postAuthForm(vertx, port, formData)
                    .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                        assertThat(response.statusCode()).isEqualTo(400);

                        server.close().onComplete(testContext.succeedingThenComplete());
                    })));
        });
    }

    @Test
    void completeAuthFlowWithAuthenticationFailure(final Vertx vertx, final VertxTestContext testContext) {
        String requestToken = "test-request-token";
        AccountTokenDO token = AccountTokenDO.builder().token(requestToken).build();

        OpenIdConnectRequest originalRequest = ImmutableOpenIdConnectRequest.builder()
                .clientId("123")
                .redirectUri("http://localhost/callback")
                .responseType("code")
                .state("state")
                .build();

        Mockito.when(openIdConnectService.createRequestToken(any(), any(), eq(DOMAIN)))
                .thenReturn(Uni.createFrom().item(token));

        Mockito.when(openIdConnectService.getRequestFromToken(eq(requestToken), any(), eq(DOMAIN)))
                .thenReturn(Uni.createFrom().item(originalRequest));

        Mockito.when(openIdConnectService.processAuthBasicToCodeFlow(any(), any(), eq(DOMAIN)))
                .thenReturn(Uni.createFrom().failure(new ServiceAuthorizationException("PW.021", "Invalid credentials")));

        int port = 8085;

        withServer(vertx, port, server -> {
            String requestUri = "/oidc/test-domain/login?" +
                    "redirect_uri=http://localhost/callback&" +
                    "token=" + requestToken;

            // get login page with CSRF token
            return sendGetRequest(vertx, port, requestUri)
                    .compose(loginResponse -> {
                        String csrfToken = extractCsrfToken(loginResponse);

                        // submit login form with wrong password
                        String formData = "identifier=testuser&password=wrongpass&requestToken=" + requestToken + "&csrfToken=" + csrfToken;

                        return postAuthForm(vertx, port, formData);
                    })
                    .compose(response -> {
                        testContext.verify(() -> {
                            assertThat(response.statusCode()).isEqualTo(400);
                        });
                        return response.body();
                    })
                    .onComplete(testContext.succeeding(body -> testContext.verify(() -> {
                        assertThat(body.toString()).contains("PW.021");

                        server.close().onComplete(testContext.succeedingThenComplete());
                    })));
        });
    }

    @Test
    void completeOtpFlowWithValidOtp(final Vertx vertx, final VertxTestContext testContext) {
        String requestToken = "test-request-token";
        AccountTokenDO token = AccountTokenDO.builder().token(requestToken).build();

        OpenIdConnectRequest originalRequest = ImmutableOpenIdConnectRequest.builder()
                .clientId("123")
                .redirectUri("http://localhost/callback")
                .responseType("code")
                .state("state")
                .build();

        AuthResponseBO authResponse = AuthResponseBO.builder()
                .token("auth-code")
                .client(ClientBO.builder().baseUrl("http://localhost").build())
                .build();

        Mockito.when(openIdConnectService.createRequestToken(any(), any(), eq(DOMAIN)))
                .thenReturn(Uni.createFrom().item(token));

        Mockito.when(openIdConnectService.getRequestFromToken(eq(requestToken), any(), eq(DOMAIN)))
                .thenReturn(Uni.createFrom().item(originalRequest));

        Mockito.when(openIdConnectService.processAuthOtpToCodeFlow(any(), any(), eq(DOMAIN)))
                .thenReturn(Uni.createFrom().item(authResponse));

        String transactionId = "otp-transaction-id";
        String trackingSession = "tracking-session";

        int port = 8086;

        withServer(vertx, port, server -> {
            String requestUri = "/oidc/test-domain/login?" +
                    "redirect_uri=http://localhost/callback&" +
                    "token=" + requestToken;

            // get login page with CSRF token
            return sendGetRequest(vertx, port, requestUri)
                    .compose(loginResponse -> {
                        String csrfToken = extractCsrfToken(loginResponse);

                        // submit OTP form with CSRF token
                        String formData = "identifier=" + transactionId + "&password=123456&requestToken=" + requestToken
                                + "&trackingSession=" + trackingSession + "&csrfToken=" + csrfToken;

                        return postOtpForm(vertx, port, formData);
                    })
                    .compose(response -> {
                        testContext.verify(() -> {
                            assertThat(response.statusCode()).isEqualTo(200);
                        });
                        return response.body();
                    })
                    .onComplete(testContext.succeeding(body -> testContext.verify(() -> {
                        String bodyStr = body.toString();

                        assertThat(bodyStr).contains("redirectUri");
                        assertThat(bodyStr).contains(authResponse.getToken().toString());

                        server.close().onComplete(testContext.succeedingThenComplete());
                    })));
        });
    }

    @Test
    void completeOtpFlowWithInvalidOtp(final Vertx vertx, final VertxTestContext testContext) {
        String requestToken = "test-request-token";
        AccountTokenDO token = AccountTokenDO.builder().token(requestToken).build();

        OpenIdConnectRequest originalRequest = ImmutableOpenIdConnectRequest.builder()
                .clientId("123")
                .redirectUri("http://localhost/callback")
                .responseType("code")
                .state("state")
                .build();

        Mockito.when(openIdConnectService.createRequestToken(any(), any(), eq(DOMAIN)))
                .thenReturn(Uni.createFrom().item(token));

        Mockito.when(openIdConnectService.getRequestFromToken(eq(requestToken), any(), eq(DOMAIN)))
                .thenReturn(Uni.createFrom().item(originalRequest));

        Mockito.when(openIdConnectService.processAuthOtpToCodeFlow(any(), any(), eq(DOMAIN)))
                .thenReturn(Uni.createFrom().failure(new ServiceAuthorizationException("OTP.001", "Invalid OTP")));

        String transactionId = "otp-transaction-id";
        String trackingSession = "tracking-session";
        int port = 8087;

        withServer(vertx, port, server -> {
            String requestUri = "/oidc/test-domain/login?" +
                    "redirect_uri=http://localhost/callback&" +
                    "token=" + requestToken;

            // get login page with CSRF token
            return sendGetRequest(vertx, port, requestUri)
                    .compose(loginResponse -> {
                        String csrfToken = extractCsrfToken(loginResponse);

                        // submit OTP form with invalid OTP
                        String formData = "identifier=" + transactionId + "&password=000000&requestToken=" + requestToken
                                + "&trackingSession=" + trackingSession + "&csrfToken=" + csrfToken;

                        return postOtpForm(vertx, port, formData);
                    })
                    .compose(response -> {
                        testContext.verify(() -> {
                            assertThat(response.statusCode()).isEqualTo(400);
                        });
                        return response.body();
                    })
                    .onComplete(testContext.succeeding(body -> testContext.verify(() -> {
                        assertThat(body.toString()).contains("OTP.001");

                        server.close().onComplete(testContext.succeedingThenComplete());
                    })));
        });
    }

    @Test
    void otpPageValidRequest(final Vertx vertx, final VertxTestContext testContext) {
        int port = 8088;

        withServer(vertx, port, server -> {
            String requestUri = "/oidc/test-domain/otp";

            return sendGetRequest(vertx, port, requestUri)
                    .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                        assertThat(response.statusCode()).isEqualTo(200);
                        assertThat(response.getHeader("Content-Type")).isEqualTo("text/html");

                        server.close().onComplete(testContext.succeedingThenComplete());
                    })));
        });
    }

    @Test
    void otpPageInvalidDomain(final Vertx vertx, final VertxTestContext testContext) {
        int port = 8089;

        withServer(vertx, port, server -> {
            String requestUri = "/oidc/invalid-domain/otp";

            return sendGetRequest(vertx, port, requestUri)
                    .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                        assertThat(response.statusCode()).isEqualTo(404);

                        server.close().onComplete(testContext.succeedingThenComplete());
                    })));
        });
    }
}
