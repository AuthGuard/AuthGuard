package com.nexblocks.authguard.jwt.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class TestIdentityServer {
    private static final Logger LOG = LoggerFactory.getLogger(TestIdentityServer.class);

    private static final String EXPECTED_CODE = "code";
    private static final String EXPECTED_CLIENT_ID = "unit-tests";
    private static final String EXPECTED_CLIENT_SECRET = "secret";
    private static final String EXPECTED_GRANT_TYPE = "authorization_code";

    private final Vertx vertx = Vertx.vertx();
    private HttpServer server;

    private int port = 7586;;
    private final TokensResponse successResponse = new TokensResponse()
            .setIdToken("id-token")
            .setAccessToken("access-token")
            .setRefreshToken("refresh-token");

    public void start() {
        System.setProperty("io.netty.tryReflectionSetAccessible", "true");

        server = vertx.createHttpServer();
        final Router router = Router.router(vertx);

        router.post("/token")
                .handler(BodyHandler.create())
                .handler(routingContext -> {
                    final HttpServerRequest request = routingContext.request();
                    final MultiMap form = request.formAttributes();

                    LOG.debug("Request form {}", form);

                    final String code = form.get("code");
                    final String clientId = form.get("client_id");
                    final String clientSecret = form.get("client_secret");
                    final String grantType = form.get("grant_type");

                    if (EXPECTED_CODE.equals(code)
                            && EXPECTED_CLIENT_ID.equals(clientId)
                            && EXPECTED_CLIENT_SECRET.equals(clientSecret)
                            && EXPECTED_GRANT_TYPE.equals(grantType)) {
                        writeJson(routingContext.response(), successResponse);
                    } else {
                        routingContext.response()
                                .setStatusCode(400)
                                .putHeader("Content-Length", "0")
                                .write("");
                    }
                });

        server.requestHandler(router)
                .listen(port, serverStartResult -> {
                    if (serverStartResult.succeeded()) {
                        LOG.info("Server started successfully");
                    } else {
                        LOG.error("Server failed to start", serverStartResult.cause());
                    }
                });
    }

    public void stop() {
        server.close();
    }

    public int getPort() {
        return port;
    }

    public TokensResponse getSuccessResponse() {
        return successResponse;
    }

    private void writeJson(final HttpServerResponse response, final TokensResponse tokensResponse) {
        final ObjectNode objectNode = new ObjectMapper().convertValue(tokensResponse, ObjectNode.class);

        final String json = objectNode.toPrettyString();
        final byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        final int contentLength = bytes.length;

        response.putHeader("Content-Length", "" + contentLength);
        response.putHeader("Content-Type", "application/json;utf-8");

        response.write(Buffer.buffer(bytes), writeResult -> {
            if (writeResult.succeeded()) {
                LOG.info("Data wrote successfully");
            } else {
                LOG.error("Failed to write data", writeResult.cause());
            }
        });
    }
}
