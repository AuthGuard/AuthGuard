package com.nexblocks.authguard.rest.access;

import com.nexblocks.authguard.api.access.AuthGuardRoles;
import com.nexblocks.authguard.api.dto.requests.AuthRequestDTO;
import com.nexblocks.authguard.service.model.AppBO;
import io.javalin.http.Context;

import java.util.Optional;

public class Requester {
    public static Optional<AppBO> getIfApp(final Context context) {
        final Object actor = context.attribute("actor");

        if (actor instanceof AppBO) {
            return Optional.of((AppBO) actor);
        }

        return Optional.empty();
    }

    public static boolean isAuthClient(final AppBO app) {
        return app.getRoles().contains(AuthGuardRoles.AUTH_CLIENT);
    }

    public static boolean authClientCanPerform(final AuthRequestDTO authRequest) {
        return authRequest.getSourceIp() == null &&
                authRequest.getUserAgent() == null;
    }
}
