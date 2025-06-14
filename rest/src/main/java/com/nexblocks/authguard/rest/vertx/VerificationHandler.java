package com.nexblocks.authguard.rest.vertx;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.access.VertxRolesAccessHandler;
import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.common.Domain;
import com.nexblocks.authguard.api.dto.entities.RequestValidationError;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.service.VerificationService;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.Collections;

@DependsOnConfiguration("verification")
public class VerificationHandler implements VertxApiHandler {
    private final VerificationService verificationService;

    @Inject
    public VerificationHandler(final VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @Override
    public void register(final Router router) {
        router.get("/domains/:domain/verification/email")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::verifyEmail);
    }

    public void verifyEmail(final RoutingContext context) {
        final String token = context.queryParam("token").get(0);

        if (token == null) {
            context.response().setStatusCode(400)
                    .end(Json.encode(new RequestValidationError(Collections.singletonList(
                            new Violation("token", ViolationType.MISSING_REQUIRED_VALUE)))
                    ));
        }

        verificationService.verifyEmail(token, Domain.fromContext(context));

        context.response().setStatusCode(204);
    }
}
