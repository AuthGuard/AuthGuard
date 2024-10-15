package com.nexblocks.authguard.rest.routes;

import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.common.Domain;
import com.nexblocks.authguard.api.dto.entities.RequestValidationError;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.VerificationApi;
import com.nexblocks.authguard.service.VerificationService;
import com.google.inject.Inject;
import io.javalin.http.Context;

import java.util.Collections;

@DependsOnConfiguration("verification")
public class VerificationRoute extends VerificationApi {
    private final VerificationService verificationService;

    @Inject
    public VerificationRoute(final VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    public void verifyEmail(final Context context) {
        final String token = context.queryParam("token");

        if (token == null) {
            context.status(400).json(new RequestValidationError(Collections.singletonList(
                    new Violation("token", ViolationType.MISSING_REQUIRED_VALUE)
            )));
        }

        verificationService.verifyEmail(token, Domain.fromContext(context));

        context.status(200);
    }
}
