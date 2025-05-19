package com.nexblocks.authguard.jwt.oauth.route;

import com.nexblocks.authguard.api.dto.entities.RequestValidationError;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.jwt.oauth.service.OAuthConst;
import io.javalin.http.Context;
import io.vavr.control.Either;
import io.vertx.ext.web.RoutingContext;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpenIdConnectRequestParser {
    public static Either<RequestValidationError, ImmutableOpenIdConnectRequest> loginPageRequestFromQueryParams(Context context) {
        String redirectUri = context.queryParam(OAuthConst.Params.RedirectUri);

        if (redirectUri == null) {
            return Either.left(new RequestValidationError(List.of(new Violation(OAuthConst.Params.RedirectUri, ViolationType.MISSING_REQUIRED_VALUE))));
        }

        String token = context.queryParam(OAuthConst.Params.Token);

        if (token == null) {
            return Either.left(new RequestValidationError(List.of(new Violation(OAuthConst.Params.RedirectUri, ViolationType.MISSING_REQUIRED_VALUE))));
        }

        ImmutableOpenIdConnectRequest request = ImmutableOpenIdConnectRequest.builder()
                .redirectUri(redirectUri)
                .requestToken(token)
                .build();

        return Either.right(request);
    }

    public static Either<RequestValidationError, ImmutableOpenIdConnectRequest> loginPageRequestFromQueryParams(RoutingContext context) {
        String redirectUri = context.queryParam(OAuthConst.Params.RedirectUri)
                .stream().findFirst().orElse(null);

        if (redirectUri == null) {
            return Either.left(new RequestValidationError(List.of(new Violation(OAuthConst.Params.RedirectUri, ViolationType.MISSING_REQUIRED_VALUE))));
        }

        String token = context.queryParam(OAuthConst.Params.Token)
                .stream().findFirst().orElse(null);

        if (token == null) {
            return Either.left(new RequestValidationError(List.of(new Violation(OAuthConst.Params.RedirectUri, ViolationType.MISSING_REQUIRED_VALUE))));
        }

        ImmutableOpenIdConnectRequest request = ImmutableOpenIdConnectRequest.builder()
                .redirectUri(redirectUri)
                .requestToken(token)
                .build();

        return Either.right(request);
    }

    public static Either<RequestValidationError, ImmutableOpenIdConnectRequest>
            authRequestFromQueryParams(Context context, String expectedResponseType) {
        Either<RequestValidationError, String> responseType =
                readResponseType(context, expectedResponseType);

        if (responseType.isLeft()) {
            return Either.left(responseType.getLeft());
        }

        Either<RequestValidationError, String> clientId = readClientId(context);

        if (clientId.isLeft()) {
            return Either.left(clientId.getLeft());
        }

        Either<RequestValidationError, List<String>> scope = readScope(context);

        if (scope.isLeft()) {
            return Either.left(scope.getLeft());
        }

        String redirectUri = context.queryParam(OAuthConst.Params.RedirectUri);

        if (redirectUri == null) {
            return Either.left(new RequestValidationError(Collections.singletonList(
                    new Violation(OAuthConst.Params.RedirectUri, ViolationType.MISSING_REQUIRED_VALUE)
            )));
        }

        String state = context.queryParam(OAuthConst.Params.State);
        String codeChallengeMethod = context.queryParam(OAuthConst.Params.CodeChallengeMethod);
        String codeChallenge = context.queryParam(OAuthConst.Params.CodeChallenge);

        ImmutableOpenIdConnectRequest.Builder request = ImmutableOpenIdConnectRequest.builder()
                .responseType(responseType.get())
                .clientId(clientId.get())
                .redirectUri(redirectUri)
                .state(state)
                .scope(scope.get())
                .codeChallenge(codeChallenge)
                .codeChallengeMethod(codeChallengeMethod);

        return Either.right(request.build());
    }

    public static Either<RequestValidationError, ImmutableOpenIdConnectRequest>
    authRequestFromQueryParams(RoutingContext context, String expectedResponseType) {
        Either<RequestValidationError, String> responseType =
                readResponseType(context, expectedResponseType);

        if (responseType.isLeft()) {
            return Either.left(responseType.getLeft());
        }

        Either<RequestValidationError, String> clientId = readClientId(context);

        if (clientId.isLeft()) {
            return Either.left(clientId.getLeft());
        }

        Either<RequestValidationError, List<String>> scope = readScope(context);

        if (scope.isLeft()) {
            return Either.left(scope.getLeft());
        }

        String redirectUri = context.queryParam(OAuthConst.Params.RedirectUri)
                .stream().findFirst().orElse(null);

        if (redirectUri == null) {
            return Either.left(new RequestValidationError(Collections.singletonList(
                    new Violation(OAuthConst.Params.RedirectUri, ViolationType.MISSING_REQUIRED_VALUE)
            )));
        }

        String state = context.queryParam(OAuthConst.Params.State)
                .stream().findFirst().orElse(null);
        String codeChallengeMethod = context.queryParam(OAuthConst.Params.CodeChallengeMethod)
                .stream().findFirst().orElse(null);
        String codeChallenge = context.queryParam(OAuthConst.Params.CodeChallenge)
                .stream().findFirst().orElse(null);

        ImmutableOpenIdConnectRequest.Builder request = ImmutableOpenIdConnectRequest.builder()
                .responseType(responseType.get())
                .clientId(clientId.get())
                .redirectUri(redirectUri)
                .state(state)
                .scope(scope.get())
                .codeChallenge(codeChallenge)
                .codeChallengeMethod(codeChallengeMethod);

        return Either.right(request.build());
    }

    private static Either<RequestValidationError, String> readResponseType(final Context context, final String expectedResponseType) {
        String responseType = context.queryParam(OAuthConst.Params.ResponseType);

        if (responseType == null) {
            return Either.left(new RequestValidationError(Collections.singletonList(
                    new Violation(OAuthConst.Params.ResponseType, ViolationType.MISSING_REQUIRED_VALUE)
            )));
        }

        if (!responseType.equals(expectedResponseType)) {
            return Either.left(new RequestValidationError(Collections.singletonList(
                    new Violation(OAuthConst.Params.ResponseType, ViolationType.INVALID_VALUE)
            )));
        }

        return Either.right(responseType);
    }

    private static Either<RequestValidationError, String> readResponseType(final RoutingContext context,
                                                                           final String expectedResponseType) {
        String responseType = context.queryParam(OAuthConst.Params.ResponseType)
                .stream().findFirst().orElse(null);

        if (responseType == null) {
            return Either.left(new RequestValidationError(Collections.singletonList(
                    new Violation(OAuthConst.Params.ResponseType, ViolationType.MISSING_REQUIRED_VALUE)
            )));
        }

        if (!responseType.equals(expectedResponseType)) {
            return Either.left(new RequestValidationError(Collections.singletonList(
                    new Violation(OAuthConst.Params.ResponseType, ViolationType.INVALID_VALUE)
            )));
        }

        return Either.right(responseType);
    }

    private static Either<RequestValidationError, String> readClientId(final Context context) {
        String clientId = context.queryParam(OAuthConst.Params.ClientId);

        if (clientId == null) {
            return Either.left(new RequestValidationError(Collections.singletonList(
                    new Violation(OAuthConst.Params.ClientId, ViolationType.MISSING_REQUIRED_VALUE)
            )));
        }

        return Either.right(clientId);
    }

    private static Either<RequestValidationError, String> readClientId(final RoutingContext context) {
        String clientId = context.queryParam(OAuthConst.Params.ClientId)
                .stream().findFirst().orElse(null);

        if (clientId == null) {
            return Either.left(new RequestValidationError(Collections.singletonList(
                    new Violation(OAuthConst.Params.ClientId, ViolationType.MISSING_REQUIRED_VALUE)
            )));
        }

        return Either.right(clientId);
    }

    private static Either<RequestValidationError, List<String>> readScope(Context context) {
        String scope = context.queryParam(OAuthConst.Params.Scope);

        if (scope == null) {
            return Either.left(new RequestValidationError(Collections.singletonList(
                    new Violation(OAuthConst.Params.Scope, ViolationType.MISSING_REQUIRED_VALUE)
            )));
        }

        return Either.right(Stream.of(scope.split(",")).collect(Collectors.toList()));
    }

    private static Either<RequestValidationError, List<String>> readScope(RoutingContext context) {
        String scope = context.queryParam(OAuthConst.Params.Scope)
                .stream().findFirst().orElse(null);

        if (scope == null) {
            return Either.left(new RequestValidationError(Collections.singletonList(
                    new Violation(OAuthConst.Params.Scope, ViolationType.MISSING_REQUIRED_VALUE)
            )));
        }

        return Either.right(Stream.of(scope.split(",")).collect(Collectors.toList()));
    }
}
