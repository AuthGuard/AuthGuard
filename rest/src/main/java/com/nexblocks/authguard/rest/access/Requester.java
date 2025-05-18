package com.nexblocks.authguard.rest.access;

import com.nexblocks.authguard.api.dto.requests.AuthRequestDTO;
import com.nexblocks.authguard.service.model.Client;
import com.nexblocks.authguard.service.model.ClientBO;
import io.javalin.http.Context;
import io.vertx.ext.web.RoutingContext;

import java.util.Optional;

public class Requester {
    public static Optional<ClientBO> getIfApp(final Context context) {
        final Object actor = context.attribute("actor");

        if (actor instanceof ClientBO) {
            return Optional.of((ClientBO) actor);
        }

        return Optional.empty();
    }

    public static Optional<ClientBO> getIfApp(final RoutingContext context) {
        final Object actor = context.get("actor");

        if (actor instanceof ClientBO) {
            return Optional.of((ClientBO) actor);
        }

        return Optional.empty();
    }

    public static boolean isAuthClient(final ClientBO app) {
        return app.getClientType() == Client.ClientType.AUTH;
    }

    public static boolean authClientCanPerform(final AuthRequestDTO authRequest) {
        return authRequest.getSourceIp() == null &&
                authRequest.getUserAgent() == null;
    }
}
