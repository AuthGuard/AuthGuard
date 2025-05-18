package com.nexblocks.authguard.rest.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import com.google.inject.Inject;
import com.nexblocks.authguard.api.common.*;
import com.nexblocks.authguard.api.dto.entities.ApiKeyDTO;
import com.nexblocks.authguard.api.dto.entities.AppDTO;
import com.nexblocks.authguard.api.dto.entities.CryptoKeyDTO;
import com.nexblocks.authguard.api.dto.requests.*;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.ApplicationsApi;
import com.nexblocks.authguard.api.common.EntityDomainChecker;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.ApplicationsService;
import com.nexblocks.authguard.service.KeyManagementService;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.PermissionBO;
import com.nexblocks.authguard.api.dto.requests.CreateAppRequestDTO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.javalin.validation.Validator;
import io.javalin.http.Context;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.*;
import static io.javalin.apibuilder.ApiBuilder.get;

public class ApplicationsRoute extends ApplicationsApi {
    private final ApplicationsService applicationsService;
    private final ApiKeysService apiKeysService;
    private final KeyManagementService keyManagementService;
    private final RestMapper restMapper;

    private final BodyHandler<CreateAppRequestDTO> appRequestRequestBodyHandler;
    private final BodyHandler<PermissionsRequestDTO> permissionsRequestBodyHandler;
    private final BodyHandler<RolesRequestDTO> rolesRequestBodyHandler;

    @Inject
    public ApplicationsRoute(final ApplicationsService applicationsService,
                             final ApiKeysService apiKeysService,
                             final KeyManagementService keyManagementService,
                             final RestMapper restMapper) {
        this.applicationsService = applicationsService;
        this.apiKeysService = apiKeysService;
        this.keyManagementService = keyManagementService;
        this.restMapper = restMapper;

        this.appRequestRequestBodyHandler = new BodyHandler.Builder<>(CreateAppRequestDTO.class)
                .build();
        this.permissionsRequestBodyHandler = new BodyHandler.Builder<>(PermissionsRequestDTO.class)
                .build();
        this.rolesRequestBodyHandler = new BodyHandler.Builder<>(RolesRequestDTO.class)
                .build();
    }

    @Override
    public String getPath() {
        return "/domains/{domain}/apps";
    }

    @Override
    public void addEndpoints() {
        post("/", this::create, ActorRoles.anyAdmin());

        get("/{id}", this::getById, ActorRoles.adminClient());
        get("/externalId/{id}", this::getByExternalId, ActorRoles.adminClient());
        put("/{id}", this::update, ActorRoles.adminClient());
        delete("/{id}", this::deleteById, ActorRoles.adminClient());

        patch("/{id}/activate", this::activate, ActorRoles.adminClient());
        patch("/{id}/deactivate", this::deactivate, ActorRoles.adminClient());
        patch("/{id}/permissions", this::updatePermissions, ActorRoles.adminClient());
        patch("/{id}/roles", this::updateRoles, ActorRoles.adminClient());

        get("/{id}/keys", this::getApiKeys, ActorRoles.adminClient());
        get("/{id}/crypto_keys", this::getCryptoKeys, ActorRoles.adminClient());
    }

    public void create(final Context context) {
        String idempotentKey = IdempotencyHeader.getKeyOrFail(context);
        CreateAppRequestDTO request = appRequestRequestBodyHandler.getValidated(context);

        RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .source(context.ip())
                .build();

        CompletableFuture<AppDTO> created = applicationsService.create(restMapper.toBO(request), requestContext)
                .thenApply(restMapper::toDTO);

        context.future(() -> created.thenAccept(r -> context.status(201).json(r)));
    }

    public void getById(final Context context) {
        Validator<Long> applicationId = context.pathParamAsClass("id", Long.class);

        if (!applicationId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<AppDTO> application = applicationsService.getById(applicationId.get(), Domain.fromContext(context))
                .thenCompose(AsyncUtils::fromAppOptional)
                .thenApply(restMapper::toDTO);

        context.future(() -> application.thenAccept(context::json));
    }

    public void getByExternalId(final Context context) {
        Validator<Long> applicationId = context.pathParamAsClass("id", Long.class);

        if (!applicationId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<AppDTO> application = applicationsService.getByExternalId(applicationId.get(), Domain.fromContext(context))
                .thenCompose(AsyncUtils::fromAppOptional)
                .thenApply(restMapper::toDTO);

        context.future(() -> application.thenAccept(context::json));
    }

    public void update(final Context context) {
        AppDTO app = RestJsonMapper.asClass(context.body(), AppDTO.class);

        EntityDomainChecker.checkEntityDomainOrFail(app, context);

        CompletableFuture<AppDTO> application = applicationsService.update(restMapper.toBO(app), Domain.fromContext(context))
                .thenCompose(AsyncUtils::fromAppOptional)
                .thenApply(restMapper::toDTO);

        context.future(() -> application.thenAccept(context::json));
    }

    public void deleteById(final Context context) {
        Validator<Long> applicationId = context.pathParamAsClass("id", Long.class);

        if (!applicationId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<AppDTO> application = applicationsService.delete(applicationId.get(), Domain.fromContext(context))
                .thenCompose(AsyncUtils::fromAppOptional)
                .thenApply(restMapper::toDTO);

        context.future(() -> application.thenAccept(context::json));
    }

    public void activate(final Context context) {
        Validator<Long> applicationId = context.pathParamAsClass("id", Long.class);

        if (!applicationId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<AppDTO> application = applicationsService.activate(applicationId.get(), Domain.fromContext(context))
                .thenApply(restMapper::toDTO);

        context.future(() -> application.thenAccept(context::json));
    }

    public void deactivate(final Context context) {
        Validator<Long> applicationId = context.pathParamAsClass("id", Long.class);

        if (!applicationId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<AppDTO> application = applicationsService.deactivate(applicationId.get(), Domain.fromContext(context))
                .thenApply(restMapper::toDTO);

        context.future(() -> application.thenAccept(context::json));
    }

    @Override
    public void updatePermissions(final Context context) {
        Validator<Long> appId = context.pathParamAsClass("id", Long.class);

        if (!appId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        PermissionsRequestDTO request = permissionsRequestBodyHandler.getValidated(context);

        List<PermissionBO> permissions = request.getPermissions().stream()
                .map(restMapper::toBO)
                .collect(Collectors.toList());

        CompletableFuture<Optional<AppBO>> updatedAccount;

        if (request.getAction() == PermissionsRequest.Action.GRANT) {
            updatedAccount = applicationsService.grantPermissions(appId.get(), permissions, Domain.fromContext(context));
        } else {
            updatedAccount = applicationsService.revokePermissions(appId.get(), permissions, Domain.fromContext(context));
        }

        context.future(() -> updatedAccount.thenCompose(AsyncUtils::fromAppOptional).thenApply(restMapper::toDTO).thenAccept(context::json));
    }

    @Override
    public void updateRoles(final Context context) {
        Validator<Long> appId = context.pathParamAsClass("id", Long.class);

        if (!appId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        RolesRequestDTO request = rolesRequestBodyHandler.getValidated(context);

        CompletableFuture<Optional<AppBO>> updatedAccount;

        if (request.getAction() == RolesRequest.Action.GRANT) {
            updatedAccount = applicationsService.grantRoles(appId.get(), request.getRoles(), Domain.fromContext(context));
        } else {
            updatedAccount = applicationsService.revokeRoles(appId.get(), request.getRoles(), Domain.fromContext(context));
        }

        context.future(() -> updatedAccount.thenCompose(AsyncUtils::fromAppOptional).thenApply(restMapper::toDTO).thenAccept(context::json));
    }

    @Override
    public void getApiKeys(final Context context) {
        Validator<Long> applicationId = context.pathParamAsClass("id", Long.class);

        if (!applicationId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<List<ApiKeyDTO>> keys = apiKeysService.getByAppId(applicationId.get(), Domain.fromContext(context))
                .thenApply(list -> list
                        .stream()
                        .map(restMapper::toDTO)
                        .collect(Collectors.toList()));

        context.future(() -> keys.thenAccept(context::json));
    }

    @Override
    public void getCryptoKeys(final Context context) {
        Validator<Long> appId = context.pathParamAsClass("id", Long.class);

        if (!appId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        String domain = Domain.fromContext(context);
        Long cursor = context.queryParamAsClass("cursor", Long.class).getOrDefault(null);
        Instant instantCursor = Cursors.parseInstantCursor(cursor);

        CompletableFuture<List<CryptoKeyDTO>> keys = keyManagementService.getByAccountId(domain, appId.get(), instantCursor)
                .thenApply(list -> list.stream()
                        .map(restMapper::toDTO)
                        .collect(Collectors.toList()));

        context.future(() -> keys.thenAccept(context::json));
    }
}
