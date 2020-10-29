package com.authguard.jwt.oauth;

import com.authguard.jwt.oauth.config.ImmutableOAuthClientConfiguration;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.random.CryptographicRandom;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

public class OAuthServiceClient {
    private static final Logger LOG = LoggerFactory.getLogger(OAuthServiceClient.class);

    private final ImmutableOAuthClientConfiguration clientConfiguration;

    private final WebClient webClient;
    private final CryptographicRandom cryptographicRandom;
    private final HttpUrl authUrl;
    private final HttpUrl tokenUrl;

    public OAuthServiceClient(final ImmutableOAuthClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;

        this.webClient = WebClient.create(VertxContext.get()); // TODO set client options
        this.cryptographicRandom = new CryptographicRandom();

        this.authUrl = HttpUrl.get(clientConfiguration.getAuthUrl())
                .newBuilder()
                .addQueryParameter("client_id", clientConfiguration.getClientId())
                .addQueryParameter("redirect_uri", clientConfiguration.getAuthRedirectUrl())
                .build();

        this.tokenUrl = HttpUrl.get(clientConfiguration.getTokenUrl());
    }

    /**
     * Creates an authorization URL to send the user to. It will get the URL
     * from {@link ImmutableOAuthClientConfiguration#getAuthUrl()}, then add
     * the following query parameters to it:
     * 1. client_id: from {@link ImmutableOAuthClientConfiguration#getClientId()}
     * 2. state: argument
     * 3. redirect_uri: from {@link ImmutableOAuthClientConfiguration#getAuthRedirectUrl()}
     * 4. response_type: argument
     * 5. scope: from {@link ImmutableOAuthClientConfiguration#getDefaultScopes()}
     * 6. nonce: randomly generated to prevent replay attacks
     */
    public String createAuthorizationUrl(final String state, final ResponseType responseType) {
        final String scopes = String.join(" ", clientConfiguration.getDefaultScopes());
        final String nonce = cryptographicRandom.base64Url(8);

        final HttpUrl url = authUrl
                .newBuilder()
                .addQueryParameter("state", state)
                .addQueryParameter("response_type", responseType.type())
                .addQueryParameter("scope", scopes)
                .addQueryParameter("nonce", nonce)
                .build();

        return url.toString();
    }

    /**
     * Sends a request to exchange the authorization code with ID, access,
     * and refresh tokens.
     */
    public CompletableFuture<TokensResponse> authorize(final String code) {
        final MultiMap form = MultiMap.caseInsensitiveMultiMap()
                .set("code", code)
                .set("client_id", clientConfiguration.getClientId())
                .set("client_secret", clientConfiguration.getClientSecret())
                .set("redirect_uri", clientConfiguration.getTokenRedirectUrl())
                .set("grant_type", GrantType.AUTHORIZATION_CODE.type());

        final String url = clientConfiguration.getTokenUrl();

        final CompletableFuture<TokensResponse> future = new CompletableFuture<>();

        webClient.post(tokenUrl.port(), tokenUrl.host(), tokenUrl.encodedPath())
                .sendForm(form, response -> {
                    if (response.succeeded()) {
                        final HttpResponse<Buffer> httpResponse = response.result();

                        if (httpResponse.statusCode() == 200) {
                            processResponse(httpResponse, url, future);
                        } else {
                            LOG.warn("Call to {} returned status code {}", url, httpResponse.statusCode());

                            future.completeExceptionally(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                                    "Unsuccessful call to the identity provider"));
                        }
                    } else {
                        LOG.error("Call to {} failed", url, response.cause());

                        future.completeExceptionally(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                                "Unexpected identity provider connection error"));
                    }
                });

        return future;
    }

    private void processResponse(final HttpResponse<Buffer> httpResponse, final String url,
                                 final CompletableFuture<TokensResponse> future) {
        final JsonArray jsonArray = httpResponse.bodyAsJsonArray();

        if (jsonArray.isEmpty()) {
            LOG.warn("An authorization code exchange call to {} returned an empty list", url);

            future.completeExceptionally(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Identity provider didn't return any tokens"));
        } else {
            try {
                final JsonObject first = jsonArray.getJsonObject(0);

                final TokensResponse tokens = new TokensResponse()
                        .setAccessToken(first.getString("access_token"))
                        .setIdToken(first.getString("id_token"))
                        .setRefreshToken(first.getString("refresh_token"));

                future.complete(tokens);
            } catch (final Exception e) {
                LOG.error("An error occurred while trying to parse the response from {}", url, e);

                future.completeExceptionally(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                        "Failed to process identity provider response"));
            }
        }
    }

    private String encode(final String value) {
        return URLEncoder.encode(value, Charset.defaultCharset());
    }
}
