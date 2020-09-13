package com.authguard.rest.routes;

import com.authguard.api.routes.VerificationApi;
import com.authguard.service.VerificationService;
import com.google.inject.Inject;
import io.javalin.http.Context;

public class VerificationRoute extends VerificationApi {
    private final VerificationService verificationService;

    @Inject
    public VerificationRoute(final VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    public void verifyEmail(final Context context) {
        final String token = context.queryParam("token");

        if (token == null) {
            context.status(400).result("Missing query parameter 'token'");
        }

        verificationService.verifyEmail(token);

        context.status(200);
    }
}
