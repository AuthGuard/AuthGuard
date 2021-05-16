package com.nexblocks.authguard.jwt.oauth.route;

import com.nexblocks.authguard.api.dto.entities.RequestValidationError;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import io.javalin.http.Context;
import io.vavr.control.Either;

import java.util.Arrays;
import java.util.Collections;

public class OpenIdConnectRequestParser {
    public static Either<RequestValidationError, ImmutableOpenIdConnectRequest> fromContext(final Context context,
                                                                                            final String expectedResponseType) {
        final Either<RequestValidationError, String> responseType =
                readResponseType(context, expectedResponseType);

        if (responseType.isLeft()) {
            return Either.left(responseType.getLeft());
        }

        final Either<RequestValidationError, String> clientId = readClientId(context);

        if (clientId.isLeft()) {
            return Either.left(clientId.getLeft());
        }

        final String redirectUri = context.queryParam("redirect_uri");
        final String scope = context.queryParam("scope");
        final String state = context.queryParam("state");

        final ImmutableOpenIdConnectRequest.Builder request = ImmutableOpenIdConnectRequest.builder()
                .responseType(responseType.get())
                .clientId(clientId.get())
                .redirectUri(redirectUri)
                .state(state);

        if (scope != null) {
            request.scope(Arrays.asList(scope.split(" ")));
        }

        return Either.right(request.build());
    }

    private static Either<RequestValidationError, String> readResponseType(final Context context, final String expectedResponseType) {
        final String responseType = context.queryParam("response_type");

        if (responseType == null) {
            return Either.left(new RequestValidationError(Collections.singletonList(
                    new Violation("response_type", ViolationType.MISSING_REQUIRED_VALUE)
            )));
        }

        if (!responseType.equals(expectedResponseType)) {
            return Either.left(new RequestValidationError(Collections.singletonList(
                    new Violation("response_type", ViolationType.INVALID_VALUE)
            )));
        }

        return Either.right(responseType);
    }

    private static Either<RequestValidationError, String> readClientId(final Context context) {
        final String clientId = context.queryParam("client_id");

        if (clientId == null) {
            return Either.left(new RequestValidationError(Collections.singletonList(
                    new Violation("client_id", ViolationType.MISSING_REQUIRED_VALUE)
            )));
        }

        return Either.right(clientId);
    }
}
