package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.common.Domain;
import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.dto.entities.ApiKeyDTO;
import com.nexblocks.authguard.api.dto.entities.AppDTO;
import com.nexblocks.authguard.api.dto.requests.ApiKeyRequestDTO;
import com.nexblocks.authguard.api.dto.requests.ApiKeyVerificationRequestDTO;
import com.nexblocks.authguard.api.dto.validation.IdParser;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.ApiKeysApi;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.ApiKeyBO;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.javalin.validation.Validator;
import io.javalin.http.Context;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class ApiKeysRoute extends ApiKeysApi {
    private final ApiKeysService apiKeysService;
    private final RestMapper restMapper;

    private final BodyHandler<ApiKeyRequestDTO> apiKeyRequestBodyHandler;
    private final BodyHandler<ApiKeyVerificationRequestDTO> verificationRequestBodyHandler;

    @Inject
    public ApiKeysRoute(final ApiKeysService apiKeysService, final RestMapper restMapper) {
        this.apiKeysService = apiKeysService;
        this.restMapper = restMapper;

        this.apiKeyRequestBodyHandler = new BodyHandler.Builder<>(ApiKeyRequestDTO.class)
                .build();
        this.verificationRequestBodyHandler = new BodyHandler.Builder<>(ApiKeyVerificationRequestDTO.class)
                .build();
    }

    @Override
    public void generate(final Context context) {
        ApiKeyRequestDTO request = apiKeyRequestBodyHandler.getValidated(context);
        Duration validFor = request.getValidFor() == null ? Duration.ZERO : request.getValidFor().toDuration();
        String domain = Domain.fromContext(context);

        CompletableFuture<ApiKeyBO> key = request.isForClient() ?
                apiKeysService.generateClientApiKey(IdParser.from(request.getAppId()), domain, request.getKeyType(),
                        request.getName(), validFor) :
                apiKeysService.generateApiKey(IdParser.from(request.getAppId()), domain, request.getKeyType(),
                        request.getName(), validFor);

        context.future(() -> key.thenApply(restMapper::toDTO).thenAccept(r -> context.status(201).json(r)));
    }

    @Override
    public void getById(final Context context) {
        Validator<Long> apiKeyId = context.pathParamAsClass("id", Long.class);

        if (!apiKeyId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<ApiKeyDTO> apiKey = apiKeysService.getById(apiKeyId.get(), Domain.fromContext(context))
                .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.API_KEY_DOES_NOT_EXIST, "API key does not exist"))
                .thenApply(restMapper::toDTO);

        context.future(() -> apiKey.thenAccept(context::json));
    }

    @Override
    public void verify(final Context context) {
        ApiKeyVerificationRequestDTO verificationRequest = verificationRequestBodyHandler.getValidated(context);

        CompletableFuture<AppDTO> app = apiKeysService.validateApiKey(verificationRequest.getKey(), Domain.fromContext(context),
                        verificationRequest.getKeyType()).thenApply(restMapper::toDTO);

        context.future(() -> app.thenAccept(context::json));
    }

    @Override
    public void deleteById(final Context context) {
        Validator<Long> apiKeyId = context.pathParamAsClass("id", Long.class);

        if (!apiKeyId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<ApiKeyDTO> apiKey = apiKeysService.delete(apiKeyId.get(), Domain.fromContext(context))
                .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.API_KEY_DOES_NOT_EXIST, "API key does not exist"))
                .thenApply(restMapper::toDTO);

        context.future(() -> apiKey.thenAccept(context::json));
    }
}
