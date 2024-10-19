package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.common.*;
import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.api.common.IdempotencyHeader;
import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.api.dto.entities.UserIdentifier;
import com.nexblocks.authguard.api.dto.entities.*;
import com.nexblocks.authguard.api.dto.requests.*;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.AccountsApi;
import com.nexblocks.authguard.rest.access.ActorDomainVerifier;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.AccountLocksService;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.ApplicationsService;
import com.nexblocks.authguard.service.KeyManagementService;
import com.nexblocks.authguard.service.model.Client;
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.javalin.validation.Validator;
import io.javalin.http.Context;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AccountsRoute extends AccountsApi {
    private final AccountsService accountsService;
    private final ApplicationsService applicationsService;
    private final AccountLocksService accountLocksService;
    private final KeyManagementService keyManagementService;
    private final RestMapper restMapper;

    private final BodyHandler<CreateAccountRequestDTO> accountRequestBodyHandler;
    private final BodyHandler<UpdateAccountRequestDTO> updateAccountRequestBodyHandler;
    private final BodyHandler<PermissionsRequestDTO> permissionsRequestBodyHandler;
    private final BodyHandler<RolesRequestDTO> rolesRequestBodyHandler;

    @Inject
    AccountsRoute(final AccountsService accountsService,
                  final ApplicationsService applicationsService,
                  final AccountLocksService accountLocksService,
                  final KeyManagementService keyManagementService,
                  final RestMapper restMapper) {
        this.accountsService = accountsService;
        this.applicationsService = applicationsService;
        this.accountLocksService = accountLocksService;
        this.keyManagementService = keyManagementService;
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

    @Override
    public void create(final Context context) {
        String idempotentKey = IdempotencyHeader.getKeyOrFail(context);
        CreateAccountRequestDTO request = accountRequestBodyHandler.getValidated(context);

        if (!canPerform(context, request)) {
            context.status(403)
                    .json(new Error("", "An auth client violated its restrictions in the request"));
            return;
        }

        RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .source(context.ip())
                .build();

        AccountBO account = restMapper.toBO(request);

        List<UserIdentifierBO> identifiers = account.getIdentifiers()
                .stream()
                .map(identifier -> identifier.withDomain(request.getDomain()))
                .collect(Collectors.toList());

        AccountBO withIdentifiers = account.withIdentifiers(identifiers);

        CompletableFuture<AccountDTO> result = accountsService.create(withIdentifiers, requestContext)
                .thenApply(restMapper::toDTO);

        context.future(() -> result.thenAccept(r -> context.status(201).json(r)));
    }

    @Override
    public void getById(final Context context) {
        Validator<Long> accountId = context.pathParamAsClass("id", Long.class);

        if (!accountId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<AccountDTO> account = accountsService.getById(accountId.get(), Domain.fromContext(context))
                .thenCompose(AsyncUtils::fromAccountOptional)
                .thenApply(restMapper::toDTO);

        context.future(() -> account.thenAccept(context::json));
    }

    @Override
    public void getByIdentifier(final Context context) {
        String identifier = context.pathParam("identifier");
        String domain = context.pathParam("domain");

        if (!ActorDomainVerifier.verifyActorDomain(context, domain)) {
            return;
        }

        CompletableFuture<AccountDTO> account = accountsService.getByIdentifier(identifier, domain)
                .thenCompose(AsyncUtils::fromAccountOptional)
                .thenApply(restMapper::toDTO);

        context.future(() -> account.thenAccept(context::json));
    }

    @Override
    public void identifierExists(final Context context) {
        String domain = context.pathParam("domain");

        if (!ActorDomainVerifier.verifyActorDomain(context, domain)) {
            return;
        }

        String identifier = context.pathParam("identifier");

        CompletableFuture<ExistsResponseDTO> exists = accountsService.getByIdentifier(identifier, domain)
                .thenCompose(AsyncUtils::fromAccountOptional)
                .thenApply(ignored -> ExistsResponseDTO.builder().success(true).build());

        context.future(() -> exists.thenAccept(context::json));
    }

    @Override
    public void deleteAccount(final Context context) {
        Validator<Long> accountId = context.pathParamAsClass("id", Long.class);

        if (!accountId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<AccountDTO> account = accountsService.delete(accountId.get(), Domain.fromContext(context))
                .thenCompose(AsyncUtils::fromAccountOptional)
                .thenApply(restMapper::toDTO);

        context.future(() -> account.thenAccept(context::json));
    }

    @Override
    public void patchAccount(final Context context) {
        Validator<Long> accountId = context.pathParamAsClass("id", Long.class);

        if (!accountId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }
        
        UpdateAccountRequestDTO request = updateAccountRequestBodyHandler.getValidated(context);

        CompletableFuture<AccountDTO> account = accountsService.patch(accountId.get(), restMapper.toBO(request), Domain.fromContext(context))
                .thenCompose(AsyncUtils::fromAccountOptional)
                .thenApply(restMapper::toDTO);

        context.future(() -> account.thenAccept(context::json));
    }

    @Override
    public void getByExternalId(final Context context) {
        String accountId = context.pathParam("id");

        CompletableFuture<AccountDTO> account = accountsService.getByExternalId(accountId, Domain.fromContext(context))
                .thenCompose(AsyncUtils::fromAccountOptional)
                .thenApply(restMapper::toDTO);

        context.future(() -> account.thenAccept(context::json));
    }

    @Override
    public void getByEmail(final Context context) {
        String domain = context.pathParam("domain");
        String email = context.pathParam("email");

        CompletableFuture<AccountDTO> account = accountsService.getByEmail(email, domain)
                .thenCompose(AsyncUtils::fromAccountOptional)
                .thenApply(restMapper::toDTO);

        context.future(() -> account.thenAccept(context::json));
    }

    @Override
    public void emailExists(final Context context) {
        String domain = context.pathParam("domain");
        String email = context.pathParam("email");

        if (!ActorDomainVerifier.verifyActorDomain(context, domain)) {
            return;
        }

        CompletableFuture<ExistsResponseDTO> exists = accountsService.getByEmail(email, domain)
                .thenCompose(AsyncUtils::fromAccountOptional)
                .thenApply(ignored -> ExistsResponseDTO.builder()
                        .success(true)
                        .build());

        context.future(() -> exists.thenAccept(context::json));
    }

    @Override
    public void updatePermissions(final Context context) {
        Validator<Long> accountId = context.pathParamAsClass("id", Long.class);

        if (!accountId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }
        
        PermissionsRequestDTO request = permissionsRequestBodyHandler.getValidated(context);

        List<PermissionBO> permissions = request.getPermissions().stream()
                .map(restMapper::toBO)
                .collect(Collectors.toList());

        CompletableFuture<Optional<AccountBO>> updatedAccount;

        if (request.getAction() == PermissionsRequest.Action.GRANT) {
            updatedAccount = accountsService.grantPermissions(accountId.get(), permissions, Domain.fromContext(context));
        } else {
            updatedAccount = accountsService.revokePermissions(accountId.get(), permissions, Domain.fromContext(context));
        }

        context.future(() -> updatedAccount.thenCompose(AsyncUtils::fromAccountOptional).thenApply(restMapper::toDTO).thenAccept(context::json));
    }

    @Override
    public void updateRoles(final Context context) {
        Validator<Long> accountId = context.pathParamAsClass("id", Long.class);

        if (!accountId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }
        
        RolesRequestDTO request = rolesRequestBodyHandler.getValidated(context);

        CompletableFuture<Optional<AccountBO>> updatedAccount;

        if (request.getAction() == RolesRequest.Action.GRANT) {
            updatedAccount = accountsService.grantRoles(accountId.get(), request.getRoles(), Domain.fromContext(context));
        } else {
            updatedAccount = accountsService.revokeRoles(accountId.get(), request.getRoles(), Domain.fromContext(context));
        }

        context.future(() -> updatedAccount.thenCompose(AsyncUtils::fromAccountOptional).thenApply(restMapper::toDTO).thenAccept(context::json));
    }

    @Override
    public void getApps(final Context context) {
        Validator<Long> accountId = context.pathParamAsClass("id", Long.class);

        if (!accountId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        Long cursor = context.queryParamAsClass("cursor", Long.class).getOrDefault(null);

        CompletableFuture<List<AppDTO>> apps = applicationsService.getByAccountId(accountId.get(), Domain.fromContext(context), cursor)
                .thenApply(list -> list.stream()
                        .map(restMapper::toDTO)
                        .collect(Collectors.toList()));

        context.future(() -> apps.thenAccept(context::json));
    }

    @Override
    public void getCryptoKeys(final Context context) {
        Validator<Long> accountId = context.pathParamAsClass("id", Long.class);

        if (!accountId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        String domain = Domain.fromContext(context);
        Long cursor = context.queryParamAsClass("cursor", Long.class).getOrDefault(null);
        Instant instantCursor = Cursors.parseInstantCursor(cursor);

        CompletableFuture<List<CryptoKeyDTO>> keys = keyManagementService.getByAccountId(domain, accountId.get(), instantCursor)
                .thenApply(list -> list.stream()
                        .map(restMapper::toDTO)
                        .collect(Collectors.toList()));

        context.future(() -> keys.thenAccept(context::json));
    }

    @Override
    public void activate(final Context context) {
        Validator<Long> accountId = context.pathParamAsClass("id", Long.class);

        if (!accountId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<AccountDTO> account = accountsService.activate(accountId.get(), Domain.fromContext(context))
                .thenCompose(AsyncUtils::fromAccountOptional)
                .thenApply(restMapper::toDTO);

        context.future(() -> account.thenAccept(context::json));
    }

    @Override
    public void deactivate(final Context context) {
        Validator<Long> accountId = context.pathParamAsClass("id", Long.class);

        if (!accountId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<AccountDTO> account = accountsService.deactivate(accountId.get(), Domain.fromContext(context))
                .thenCompose(AsyncUtils::fromAccountOptional)
                .thenApply(restMapper::toDTO);

        context.future(() -> account.thenAccept(context::json));
    }

    @Override
    public void getActiveLocks(final Context context) {
        Validator<Long> accountId = context.pathParamAsClass("id", Long.class);

        if (!accountId.hasValue()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        CompletableFuture<Collection<AccountLockDTO>> locks = accountLocksService.getActiveLocksByAccountId(accountId.get())
                .thenApply(list -> list.stream()
                        .map(restMapper::toDTO)
                        .collect(Collectors.toList()));

        context.future(() -> locks.thenAccept(context::json));
    }

    private boolean canPerform(final Context context, CreateAccountRequestDTO request) {
        if (context.attribute("actor") instanceof ClientBO) {
            ClientBO actor = context.attribute("actor");
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
