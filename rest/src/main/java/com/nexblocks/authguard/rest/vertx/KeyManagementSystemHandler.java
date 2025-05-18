package com.nexblocks.authguard.rest.vertx;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.access.VertxRolesAccessHandler;
import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.api.common.Cursors;
import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.dto.requests.CryptoKeyRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.KeyManagementService;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.EphemeralKeyBO;
import com.nexblocks.authguard.service.model.PersistedKeyBO;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class KeyManagementSystemHandler implements VertxApiHandler {
    private final KeyManagementService keyManagementService;
    private final RestMapper restMapper;
    private final BodyHandler<CryptoKeyRequestDTO> requestBodyHandler;

    @Inject
    public KeyManagementSystemHandler(final KeyManagementService keyManagementService, final RestMapper restMapper) {
        this.keyManagementService = keyManagementService;
        this.restMapper = restMapper;
        this.requestBodyHandler = new BodyHandler.Builder<>(CryptoKeyRequestDTO.class).build();
    }

    public void register(final Router router) {
        router.post("/domains/:domain/kms/generator")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::generate);

        router.get("/domains/:domain/kms/keys")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::getByDomain);

        router.get("/domains/:domain/kms/keys/:id")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::getById);

        router.delete("/domains/:domain/kms/keys/:id")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::deleteById);
    }

    private void generate(final RoutingContext context) {
        try {
            String domain = context.pathParam("domain");
            CryptoKeyRequestDTO request = requestBodyHandler.getValidated(context);

            EphemeralKeyBO key = keyManagementService.generate(request.getAlgorithm().name(), request.getSize());

            if (!request.isPersist()) {
                context.response().putHeader("Content-Type", "application/json")
                        .end(Json.encode(restMapper.toDTO(key)));
                return;
            }

            PersistedKeyBO toPersist = PersistedKeyBO.builder()
                    .domain(domain)
                    .algorithm(key.getAlgorithm())
                    .name(request.getName())
                    .accountId(request.getAccountId())
                    .appId(request.getAppId())
                    .size(key.getSize())
                    .privateKey(key.getPrivateKey())
                    .publicKey(key.getPublicKey())
                    .passcodeProtected(request.isPasscodeProtected())
                    .passcode(request.getPasscode())
                    .build();

            keyManagementService.create(toPersist)
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().setStatusCode(201)
                                .putHeader("Content-Type", "application/json")
                                .end(Json.encode(res));
                    });
        } catch (Exception e) {
            context.fail(e);
        }
    }

    private void getByDomain(final RoutingContext context) {
        try {
            String domain = context.pathParam("domain");
            Long cursor = Cursors.getLongCursor(context);
            Instant instantCursor = Cursors.parseInstantCursor(cursor);

            keyManagementService.getByDomain(domain, instantCursor)
                    .thenApply(list -> list.stream().map(restMapper::toDTO).collect(Collectors.toList()))
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().putHeader("Content-Type", "application/json")
                                .end(Json.encode(res));
                    });
        } catch (Exception e) {
            context.fail(e);
        }
    }

    private void getById(final RoutingContext context) {
        try {
            Long keyId = Long.valueOf(context.pathParam("id"));
            String domain = context.pathParam("domain");
            String decrypt = context.queryParam("decrypt").stream().findFirst().orElse(null);
            String passcode = context.queryParam("passcode").stream().findFirst().orElse(null);

            CompletableFuture<Optional<PersistedKeyBO>> retrieveFuture =
                    Objects.equals(decrypt, "1")
                            ? keyManagementService.getDecrypted(keyId, domain, passcode)
                            : keyManagementService.getById(keyId, domain);

            retrieveFuture
                    .thenApply(opt -> opt.map(restMapper::toDTO)
                            .orElseThrow(() -> new ServiceNotFoundException(
                                    ErrorCode.CRYPTO_KEY_DOES_NOT_EXIST, "Key does not exist")))
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().putHeader("Content-Type", "application/json")
                                .end(Json.encode(res));
                    });
        } catch (NumberFormatException e) {
            context.fail(new RequestValidationException(Collections.singletonList(
                    new Violation("id", ViolationType.INVALID_VALUE))));
        }
    }

    private void deleteById(final RoutingContext context) {
        try {
            Long keyId = Long.valueOf(context.pathParam("id"));
            String domain = context.pathParam("domain");

            keyManagementService.delete(keyId, domain)
                    .thenCompose(opt -> AsyncUtils.fromOptional(opt, ErrorCode.CRYPTO_KEY_DOES_NOT_EXIST, "Key does not exist"))
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().putHeader("Content-Type", "application/json")
                                .end(Json.encode(res));
                    });
        } catch (NumberFormatException e) {
            context.fail(new RequestValidationException(Collections.singletonList(
                    new Violation("id", ViolationType.INVALID_VALUE))));
        }
    }
}

