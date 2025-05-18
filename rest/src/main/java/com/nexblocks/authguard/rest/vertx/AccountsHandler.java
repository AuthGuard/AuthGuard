package com.nexblocks.authguard.rest.vertx;

import com.nexblocks.authguard.api.access.AuthGuardRoles;
import com.nexblocks.authguard.api.access.VertxRolesAccessHandler;
import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.api.common.Cursors;
import com.nexblocks.authguard.api.common.IdempotencyHeader;
import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.dto.entities.*;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.api.dto.entities.UserIdentifier;
import com.nexblocks.authguard.api.dto.requests.CreateAccountRequestDTO;
import com.nexblocks.authguard.api.dto.requests.PermissionsRequestDTO;
import com.nexblocks.authguard.api.dto.requests.RolesRequestDTO;
import com.nexblocks.authguard.api.dto.requests.UpdateAccountRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.rest.access.ActorDomainVerifier;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.*;
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.model.Client;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AccountsHandler implements VertxApiHandler {
    private final AccountsService accountsService;
    private final ApplicationsService applicationsService;
    private final AccountLocksService accountLocksService;
    private final KeyManagementService keyManagementService;
    private final TrackingSessionsService trackingSessionsService;
    private final RestMapper restMapper;

    private final BodyHandler<CreateAccountRequestDTO> accountRequestBodyHandler;
    private final BodyHandler<UpdateAccountRequestDTO> updateAccountRequestBodyHandler;
    private final BodyHandler<PermissionsRequestDTO> permissionsRequestBodyHandler;
    private final BodyHandler<RolesRequestDTO> rolesRequestBodyHandler;

    @Inject
    public AccountsHandler(final AccountsService accountsService, final ApplicationsService applicationsService, final AccountLocksService accountLocksService, final KeyManagementService keyManagementService, final TrackingSessionsService trackingSessionsService, final RestMapper restMapper) {
        this.accountsService = accountsService;
        this.applicationsService = applicationsService;
        this.accountLocksService = accountLocksService;
        this.keyManagementService = keyManagementService;
        this.trackingSessionsService = trackingSessionsService;
        this.restMapper = restMapper;

        this.accountRequestBodyHandler = new BodyHandler.Builder<>(CreateAccountRequestDTO.class)
                .build();
        this.updateAccountRequestBodyHandler = new BodyHandler.Builder<>(UpdateAccountRequestDTO.class)
                .build();
        this.permissionsRequestBodyHandler = new BodyHandler.Builder<>(PermissionsRequestDTO.class)
                .build();
        this.rolesRequestBodyHandler = new BodyHandler.Builder<>(RolesRequestDTO.class)
                .build();
    }

    public void register(Router router) {
        router.post("/domains/:domain/accounts")
                .handler(VertxRolesAccessHandler.forRoles(
                        AuthGuardRoles.ONE_TIME_ADMIN_ACCOUNT,
                        AuthGuardRoles.ADMIN_CLIENT,
                        AuthGuardRoles.ADMIN_ACCOUNT,
                        AuthGuardRoles.AUTH_CLIENT))
                .handler(this::createAccount);

        router.get("/domains/:domain/accounts/:id")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::getById);
        router.patch("/domains/:domain/accounts/:id/permissions")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::updatePermissions);
        router.patch("/domains/:domain/accounts/:id/roles")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::updateRoles);

        router.delete("/domains/:domain/accounts/:id").handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::deleteAccount);
        router.patch("/domains/:domain/accounts/:id").handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::patchAccount);
        router.get("/domains/:domain/accounts/identifier/:identifier")
                .handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::getByIdentifier);
        router.get("/domains/:domain/accounts/identifier/:identifier/exists")
                .handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::identifierExists);
        router.get("/domains/:domain/accounts/externalId/:id")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::getByExternalId);
        router.get("/domains/:domain/accounts/email/:email").handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::getByEmail);
        router.get("/domains/:domain/accounts/email/:email/exists")
                .handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::emailExists);
        router.patch("/domains/:domain/accounts/:id/permissions")
                .handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::updatePermissions);
        router.patch("/domains/:domain/accounts/:id/roles")
                .handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::updateRoles);
        router.get("/domains/:domain/accounts/:id/apps")
                .handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::getApps);
        router.get("/domains/:domain/accounts/:id/crypto_keys")
                .handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::getCryptoKeys);
        router.get("/domains/:domain/accounts/:id/sessions")
                .handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::getSessions);
        router.patch("/domains/:domain/accounts/:id/activate")
                .handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::activate);
        router.patch("/domains/:domain/accounts/:id/deactivate")
                .handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::deactivate);
        router.get("/domains/:domain/accounts/:id/locks")
                .handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::getActiveLocks);
    }

    private void createAccount(RoutingContext context) {
        String idempotentKey = IdempotencyHeader.getKeyOrFail(context);
        CreateAccountRequestDTO request = accountRequestBodyHandler.getValidated(context);

        if (!canPerform(context, request)) {
            context.response()
                    .setStatusCode(403)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(new Error("", "An auth client violated its restrictions in the request")));

            return;
        }

        RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .source(context.request().remoteAddress().host())
                .build();

        AccountBO account = restMapper.toBO(request);

        List<UserIdentifierBO> identifiers = account.getIdentifiers()
                .stream()
                .map(identifier -> identifier.withDomain(request.getDomain()))
                .collect(Collectors.toList());

        AccountBO withIdentifiers = account.withIdentifiers(identifiers);

        CompletableFuture<AccountDTO> future = accountsService.create(withIdentifiers, requestContext)
                .thenApply(restMapper::toDTO);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                context.fail(ex);
            } else {
                context.response()
                        .setStatusCode(201)
                        .putHeader("Content-Type", "application/json")
                        .end(Json.encode(result));
            }
        });
    }

    private void getById(RoutingContext context) {
        long id = Long.parseLong(context.pathParam("id"));
        String domain = context.pathParam("domain");

        accountsService.getById(id, domain)
                .thenApply(opt -> opt.map(restMapper::toDTO))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        context.fail(ex);
                    } else if (result.isEmpty()) {
                        context.response().setStatusCode(404).end();
                    } else {
                        context.response()
                                .putHeader("Content-Type", "application/json")
                                .end(Json.encode(result.get()));
                    }
                });
    }

    private void getByIdentifier(final RoutingContext context) {
        String identifier = context.pathParam("identifier");
        String domain = context.pathParam("domain");

        if (!ActorDomainVerifier.verifyActorDomain(context, domain)) {
            context.response().setStatusCode(403).end();
            return;
        }

        accountsService.getByIdentifier(identifier, domain)
                .thenCompose(AsyncUtils::fromAccountOptional)
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
    }

    private void identifierExists(final RoutingContext context) {
        String domain = context.pathParam("domain");

        if (!ActorDomainVerifier.verifyActorDomain(context, domain)) {
            context.response().setStatusCode(403).end();
            return;
        }

        String identifier = context.pathParam("identifier");

        accountsService.getByIdentifier(identifier, domain)
                .thenCompose(AsyncUtils::fromAccountOptional)
                .thenApply(ignored -> ExistsResponseDTO.builder().success(true).build())
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        context.fail(ex);
                    } else {
                        context.response()
                                .putHeader("Content-Type", "application/json")
                                .end(Json.encode(res));
                    }
                });
    }

    private void deleteAccount(final RoutingContext context) {
        try {
            final long accountId = Long.parseLong(context.pathParam("id"));
            final String domain = context.pathParam("domain");

            accountsService.delete(accountId, domain)
                    .thenCompose(AsyncUtils::fromAccountOptional)
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

    private void updatePermissions(RoutingContext context) {
        PermissionsRequestDTO request = permissionsRequestBodyHandler.getValidated(context);
        long accountId = Long.parseLong(context.pathParam("id"));
        String domain = context.pathParam("domain");

        List<PermissionBO> permissions = request.getPermissions().stream()
                .map(restMapper::toBO)
                .collect(Collectors.toList());

        CompletableFuture<Optional<AccountBO>> updated;

        if (request.getAction() == PermissionsRequestDTO.Action.GRANT) {
            updated = accountsService.grantPermissions(accountId, permissions, domain);
        } else {
            updated = accountsService.revokePermissions(accountId, permissions, domain);
        }

        updated.whenComplete((result, ex) -> {
            if (ex != null) {
                context.fail(ex);
            } else if (result.isEmpty()) {
                context.response().setStatusCode(404).end();
            } else {
                context.response()
                        .putHeader("Content-Type", "application/json")
                        .end(Json.encode(restMapper.toDTO(result.get())));
            }
        });
    }

    private void updateRoles(RoutingContext context) {
        RolesRequestDTO request = rolesRequestBodyHandler.getValidated(context);
        long accountId = Long.parseLong(context.pathParam("id"));
        String domain = context.pathParam("domain");

        CompletableFuture<Optional<AccountBO>> updated;

        if (request.getAction() == RolesRequestDTO.Action.GRANT) {
            updated = accountsService.grantRoles(accountId, request.getRoles(), domain);
        } else {
            updated = accountsService.revokeRoles(accountId, request.getRoles(), domain);
        }

        updated.whenComplete((result, ex) -> {
            if (ex != null) {
                context.fail(ex);
            } else if (result.isEmpty()) {
                context.response().setStatusCode(404).end();
            } else {
                context.response()
                        .putHeader("Content-Type", "application/json")
                        .end(Json.encode(restMapper.toDTO(result.get())));
            }
        });
    }

    private void patchAccount(final RoutingContext context) {
        try {
            final long id = Long.parseLong(context.pathParam("id"));
            final String domain = context.pathParam("domain");
            final UpdateAccountRequestDTO request = updateAccountRequestBodyHandler.getValidated(context);

            accountsService.patch(id, restMapper.toBO(request), domain)
                    .thenCompose(AsyncUtils::fromAccountOptional)
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().end(Json.encode(res));
                    });
        } catch (Exception e) {
            context.fail(e);
        }
    }

    private void getByExternalId(final RoutingContext context) {
        final String id = context.pathParam("id");
        final String domain = context.pathParam("domain");

        accountsService.getByExternalId(id, domain)
                .thenCompose(AsyncUtils::fromAccountOptional)
                .thenApply(restMapper::toDTO)
                .whenComplete((res, ex) -> {
                    if (ex != null) context.fail(ex);
                    else context.response().end(Json.encode(res));
                });
    }

    private void getByEmail(final RoutingContext context) {
        final String email = context.pathParam("email");
        final String domain = context.pathParam("domain");

        accountsService.getByEmail(email, domain)
                .thenCompose(AsyncUtils::fromAccountOptional)
                .thenApply(restMapper::toDTO)
                .whenComplete((res, ex) -> {
                    if (ex != null) context.fail(ex);
                    else context.response().end(Json.encode(res));
                });
    }

    private void emailExists(final RoutingContext context) {
        final String domain = context.pathParam("domain");
        final String email = context.pathParam("email");

        if (!ActorDomainVerifier.verifyActorDomain(context, domain)) {
            context.response().setStatusCode(403).end();
            return;
        }

        accountsService.getByEmail(email, domain)
                .thenCompose(AsyncUtils::fromAccountOptional)
                .thenApply(ignored -> ExistsResponseDTO.builder().success(true).build())
                .whenComplete((res, ex) -> {
                    if (ex != null) context.fail(ex);
                    else context.response().end(Json.encode(res));
                });
    }

    private void getApps(final RoutingContext context) {
        try {
            final long id = Long.parseLong(context.pathParam("id"));
            final String domain = context.pathParam("domain");
            final Long cursor = Cursors.getLongCursor(context);

            applicationsService.getByAccountId(id, domain, cursor)
                    .thenApply(list -> list.stream().map(restMapper::toDTO).collect(Collectors.toList()))
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().end(Json.encode(res));
                    });
        } catch (Exception e) {
            context.fail(e);
        }
    }

    private void getCryptoKeys(final RoutingContext context) {
        try {
            final long id = Long.parseLong(context.pathParam("id"));
            final String domain = context.pathParam("domain");
            final Instant instantCursor = Cursors.parseInstantCursor(context);

            keyManagementService.getByAccountId(domain, id, instantCursor)
                    .thenApply(list -> list.stream().map(restMapper::toDTO).collect(Collectors.toList()))
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().end(Json.encode(res));
                    });
        } catch (Exception e) {
            context.fail(e);
        }
    }

    private void getSessions(final RoutingContext context) {
        try {
            final long id = Long.parseLong(context.pathParam("id"));
            final String domain = context.pathParam("domain");

            trackingSessionsService.getByAccountId(id, domain)
                    .thenApply(list -> CollectionResponseDTO.<SessionDTO>builder()
                            .items(list.stream().map(restMapper::toDTO).collect(Collectors.toList()))
                            .build())
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().end(Json.encode(res));
                    });
        } catch (Exception e) {
            context.fail(e);
        }
    }

    private void activate(final RoutingContext context) {
        try {
            final long id = Long.parseLong(context.pathParam("id"));
            final String domain = context.pathParam("domain");

            accountsService.activate(id, domain)
                    .thenCompose(AsyncUtils::fromAccountOptional)
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().end(Json.encode(res));
                    });
        } catch (Exception e) {
            context.fail(e);
        }
    }

    private void deactivate(final RoutingContext context) {
        try {
            final long id = Long.parseLong(context.pathParam("id"));
            final String domain = context.pathParam("domain");

            accountsService.deactivate(id, domain)
                    .thenCompose(AsyncUtils::fromAccountOptional)
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().end(Json.encode(res));
                    });
        } catch (Exception e) {
            context.fail(e);
        }
    }

    private void getActiveLocks(final RoutingContext context) {
        try {
            final long id = Long.parseLong(context.pathParam("id"));

            accountLocksService.getActiveLocksByAccountId(id)
                    .thenApply(list -> list.stream().map(restMapper::toDTO).collect(Collectors.toList()))
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().end(Json.encode(res));
                    });
        } catch (Exception e) {
            context.fail(e);
        }
    }

    private boolean canPerform(final RoutingContext context, CreateAccountRequestDTO request) {
        if (context.get("actor") instanceof ClientBO) {
            ClientBO actor = context.get("actor");
            boolean isAuthClient = actor.getClientType() == Client.ClientType.AUTH;

            /*
             * Clients shouldn't have both auth and admin client
             * roles. If that was the case then it'll still
             * be treated as an auth client and not an admin client.
             */
            return !isAuthClient || canPerform(actor, request);
        }

        return true;
    }

    private boolean canPerform(final ClientBO actor, CreateAccountRequestDTO request) {
        if (request.getEmail() != null && request.getEmail().isVerified()) {
            return false;
        }

        if (request.getBackupEmail() != null && request.getBackupEmail().isVerified()) {
            return false;
        }

        if (request.getPhoneNumber() != null && request.getPhoneNumber().isVerified()) {
            return false;
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            return false;
        }

        if (actor.getDomain() == null || !actor.getDomain().equals(request.getDomain())) {
            return false;
        }

        /*
         * If identifiers are set, then only a single USERNAME identifier
         * is allowed.
         */
        if (request.getIdentifiers() != null) {
            if (request.getIdentifiers().size() != 1) {
                return false;
            }

            UserIdentifierDTO identifier = request.getIdentifiers().get(0);

            if (identifier.getType() != UserIdentifier.Type.USERNAME) {
                return false;
            }
        }

        return request.getPermissions() == null || request.getPermissions().isEmpty();
    }
}
