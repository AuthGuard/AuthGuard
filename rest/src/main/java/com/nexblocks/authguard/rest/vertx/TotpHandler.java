package com.nexblocks.authguard.rest.vertx;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.access.VertxRolesAccessHandler;
import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.dto.entities.CollectionResponseDTO;
import com.nexblocks.authguard.api.dto.entities.TotpKeyDTO;
import com.nexblocks.authguard.api.dto.requests.TotpKeyRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.TotpKeysService;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.Collections;
import java.util.stream.Collectors;

@DependsOnConfiguration("totpAuthenticators")
public class TotpHandler implements VertxApiHandler {
    private final TotpKeysService totpKeysService;
    private final RestMapper restMapper;
    private final BodyHandler<TotpKeyRequestDTO> requestBodyHandler;

    @Inject
    public TotpHandler(final TotpKeysService totpKeysService, final RestMapper restMapper) {
        this.totpKeysService = totpKeysService;
        this.restMapper = restMapper;
        this.requestBodyHandler = new BodyHandler.Builder<>(TotpKeyRequestDTO.class).build();
    }

    public void register(final Router router) {
        router.post("/domains/:domain/totp/generate")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::generate);

        router.get("/domains/:domain/totp/:accountId/keys")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::getByAccountId);

        router.delete("/domains/:domain/totp/keys/:id")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::deleteById);
    }

    private void generate(final RoutingContext context) {
        try {
            String domain = context.pathParam("domain");
            TotpKeyRequestDTO request = requestBodyHandler.getValidated(context);

            totpKeysService.generate(request.getAccountId(), domain, request.getAuthenticator())
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

    private void getByAccountId(final RoutingContext context) {
        try {
            long accountId = Long.parseLong(context.pathParam("accountId"));
            String domain = context.pathParam("domain");

            totpKeysService.getByAccountId(accountId, domain)
                    .thenApply(list -> CollectionResponseDTO.<TotpKeyDTO>builder()
                            .items(list.stream().map(restMapper::toDTO).collect(Collectors.toList()))
                            .build())
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().putHeader("Content-Type", "application/json")
                                .end(Json.encode(res));
                    });
        } catch (NumberFormatException e) {
            context.fail(new RequestValidationException(Collections.singletonList(new Violation("accountId", ViolationType.INVALID_VALUE))));
        }
    }

    private void deleteById(final RoutingContext context) {
        try {
            Long id = Long.valueOf(context.pathParam("id"));
            String domain = context.pathParam("domain");

            totpKeysService.delete(id, domain)
                    .thenApply(list -> CollectionResponseDTO.<TotpKeyDTO>builder()
                            .items(list.stream().map(restMapper::toDTO).collect(Collectors.toList()))
                            .build())
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().putHeader("Content-Type", "application/json")
                                .end(Json.encode(res));
                    });
        } catch (NumberFormatException e) {
            context.fail(new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE))));
        }
    }
}

