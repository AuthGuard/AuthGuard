package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.dto.requests.ApiKeyRequestDTO;
import com.nexblocks.authguard.api.dto.requests.ApiKeyVerificationRequestDTO;
import com.nexblocks.authguard.api.dto.validation.IdParser;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.api.access.VertxRolesAccessHandler;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.ApiKeyBO;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ApiKeysHandler implements VertxApiHandler {
    private final ApiKeysService apiKeysService;
    private final RestMapper restMapper;

    private final BodyHandler<ApiKeyRequestDTO> apiKeyRequestBodyHandler;
    private final BodyHandler<ApiKeyVerificationRequestDTO> verificationRequestBodyHandler;

    @Inject
    public ApiKeysHandler(final ApiKeysService apiKeysService, final RestMapper restMapper) {
        this.apiKeysService = apiKeysService;
        this.restMapper = restMapper;

        this.apiKeyRequestBodyHandler = new BodyHandler.Builder<>(ApiKeyRequestDTO.class).build();
        this.verificationRequestBodyHandler = new BodyHandler.Builder<>(ApiKeyVerificationRequestDTO.class).build();
    }

    public void register(final Router router) {
        router.post("/domains/:domain/keys")
                .handler(VertxRolesAccessHandler.anyAdmin())
                .handler(this::generate);

        router.get("/domains/:domain/keys/:id")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::getById);

        router.post("/domains/:domain/keys/verify")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::verify);

        router.delete("/domains/:domain/keys/:id")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::deleteById);
    }

    private void generate(final RoutingContext context) {
        try {
            final ApiKeyRequestDTO request = apiKeyRequestBodyHandler.getValidated(context);
            final Duration validFor = request.getValidFor() == null
                    ? Duration.ZERO
                    : request.getValidFor().toDuration();
            final String domain = context.pathParam("domain");

            CompletableFuture<ApiKeyBO> keyFuture = request.isForClient()
                    ? apiKeysService.generateClientApiKey(
                    IdParser.from(request.getAppId()),
                    domain,
                    request.getKeyType(),
                    request.getName(),
                    validFor)
                    : apiKeysService.generateApiKey(
                    IdParser.from(request.getAppId()),
                    domain,
                    request.getKeyType(),
                    request.getName(),
                    validFor);

            keyFuture
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            context.fail(ex);
                        } else {
                            context.response()
                                    .setStatusCode(201)
                                    .putHeader("Content-Type", "application/json")
                                    .end(Json.encode(res));
                        }
                    });
        } catch (Exception e) {
            context.fail(e);
        }
    }

    private void getById(final RoutingContext context) {
        try {
            final Long id = Long.valueOf(context.pathParam("id"));
            final String domain = context.pathParam("domain");

            apiKeysService.getById(id, domain)
                    .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.API_KEY_DOES_NOT_EXIST, "API key does not exist"))
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            context.fail(ex);
                        } else {
                            context.response()
                                    .putHeader("Content-Type", "application/json")
                                    .end(Json.encode(res));
                        }
                    });
        } catch (NumberFormatException e) {
            List<Violation> violations = Collections.singletonList(
                    new Violation("id", ViolationType.INVALID_VALUE)
            );
            context.fail(new RequestValidationException(violations));
        }
    }

    private void verify(final RoutingContext context) {
        try {
            final ApiKeyVerificationRequestDTO request = verificationRequestBodyHandler.getValidated(context);
            final String domain = context.pathParam("domain");

            apiKeysService.validateApiKey(request.getKey(), domain, request.getKeyType())
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            context.fail(ex);
                        } else {
                            context.response()
                                    .putHeader("Content-Type", "application/json")
                                    .end(Json.encode(res));
                        }
                    });
        } catch (Exception e) {
            context.fail(e);
        }
    }

    private void deleteById(final RoutingContext context) {
        try {
            final long id = Long.parseLong(context.pathParam("id"));
            final String domain = context.pathParam("domain");

            apiKeysService.delete(id, domain)
                    .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.API_KEY_DOES_NOT_EXIST, "API key does not exist"))
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            context.fail(ex);
                        } else {
                            context.response()
                                    .putHeader("Content-Type", "application/json")
                                    .end(Json.encode(res));
                        }
                    });
        } catch (NumberFormatException e) {
            List<Violation> violations = Collections.singletonList(
                    new Violation("id", ViolationType.INVALID_VALUE)
            );
            context.fail(new RequestValidationException(violations));
        }
    }
}
