package com.authguard.rest.routes;

import com.authguard.rest.access.ActorRoles;
import com.authguard.service.VerificationService;
import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.post;

public class VerificationRoute implements EndpointGroup {
    private final VerificationService verificationService;

    @Inject
    public VerificationRoute(final VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @Override
    public void addEndpoints() {
        post("/email", this::verifyEmail, ActorRoles.adminClient());
    }

    private void verifyEmail(final Context context) {
        final String token = context.queryParam("token");

        if (token == null) {
            context.status(400).result("Missing query parameter 'token'");
        }

        verificationService.verifyEmail(token);

        context.status(200);
    }
}
