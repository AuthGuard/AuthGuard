package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.access.AuthGuardRoles;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.api.dto.entities.UserIdentifier;
import com.nexblocks.authguard.api.dto.entities.*;
import com.nexblocks.authguard.api.dto.requests.*;
import com.nexblocks.authguard.api.routes.AccountsApi;
import com.nexblocks.authguard.rest.access.ActorDomainVerifier;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.util.BodyHandler;
import com.nexblocks.authguard.rest.util.IdempotencyHeader;
import com.nexblocks.authguard.service.AccountLocksService;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.ApplicationsService;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.*;
import io.javalin.http.Context;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AccountsRoute extends AccountsApi {
    private final AccountsService accountsService;
    private final ApplicationsService applicationsService;
    private final AccountLocksService accountLocksService;
    private final RestMapper restMapper;

    private final BodyHandler<CreateAccountRequestDTO> accountRequestBodyHandler;
    private final BodyHandler<UpdateAccountRequestDTO> updateAccountRequestBodyHandler;
    private final BodyHandler<PermissionsRequestDTO> permissionsRequestBodyHandler;
    private final BodyHandler<RolesRequestDTO> rolesRequestBodyHandler;

    @Inject
    AccountsRoute(final AccountsService accountsService,
                  final ApplicationsService applicationsService,
                  final AccountLocksService accountLocksService,
                  final RestMapper restMapper) {
        this.accountsService = accountsService;
        this.applicationsService = applicationsService;
        this.accountLocksService = accountLocksService;
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
        final String idempotentKey = IdempotencyHeader.getKeyOrFail(context);
        final CreateAccountRequestDTO request = accountRequestBodyHandler.getValidated(context);

        if (!canPerform(context, request)) {
            context.status(403)
                    .json(new Error("", "An auth client violated its restrictions in the request"));
            return;
        }

        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .source(context.ip())
                .build();

        final AccountBO account = restMapper.toBO(request);

        final List<UserIdentifierBO> identifiers = account.getIdentifiers()
                .stream()
                .map(identifier -> identifier.withDomain(request.getDomain()))
                .collect(Collectors.toList());

        final Optional<AccountDTO> createdAccount = Optional.of(account.withIdentifiers(identifiers))
                .map(accountBO -> accountsService.create(accountBO, requestContext))
                .map(restMapper::toDTO);

        if (createdAccount.isPresent()) {
            context.status(201).json(createdAccount.get());
        } else {
            context.status(400).json(new Error("400", "Failed to create account"));
        }
    }

    @Override
    public void getById(final Context context) {
        final String accountId = context.pathParam("id");

        final Optional<AccountDTO> account = accountsService.getById(accountId)
                .map(restMapper::toDTO);

        if (account.isPresent()) {
            context.status(200).json(account.get());
        } else {
            context.status(404)
                    .json(new Error(ErrorCode.ACCOUNT_DOES_NOT_EXIST.getCode(), "Account not found"));
        }
    }

    @Override
    public void getByIdentifier(final Context context) {
        final String identifier = context.pathParam("identifier");
        final String domain = context.pathParam("domain");

        final Optional<AccountBO> account = accountsService.getByIdentifier(identifier, domain);

        if (account.isPresent()) {
            context.status(200).json(account.get());
        } else {
            context.status(404)
                    .json(new Error(ErrorCode.ACCOUNT_DOES_NOT_EXIST.getCode(), "Account not found"));
        }
    }

    @Override
    public void identifierExists(final Context context) {
        final String domain = context.pathParam("domain");

        if (!ActorDomainVerifier.verifyActorDomain(context, domain)) {
            return;
        }

        final String identifier = context.pathParam("identifier");

        final boolean exists = accountsService.getByIdentifier(identifier, domain)
                .isPresent();

        if (exists) {
            context.status(200);
        } else {
            context.status(404);
        }
    }

    @Override
    public void deleteAccount(final Context context) {
        final String accountId = context.pathParam("id");

        final Optional<AccountDTO> account = accountsService.delete(accountId)
                .map(restMapper::toDTO);

        if (account.isPresent()) {
            context.status(200).json(account.get());
        } else {
            context.status(404)
                    .json(new Error(ErrorCode.ACCOUNT_DOES_NOT_EXIST.getCode(), "Account not found"));
        }
    }

    @Override
    public void patchAccount(final Context context) {
        final String accountId = context.pathParam("id");
        final UpdateAccountRequestDTO request = updateAccountRequestBodyHandler.getValidated(context);

        final Optional<AccountDTO> account = accountsService.patch(accountId, restMapper.toBO(request))
                .map(restMapper::toDTO);

        if (account.isPresent()) {
            context.status(200).json(account.get());
        } else {
            context.status(404)
                    .json(new Error(ErrorCode.ACCOUNT_DOES_NOT_EXIST.getCode(), "Account not found"));
        }
    }

    @Override
    public void getByExternalId(final Context context) {
        final String accountId = context.pathParam("id");

        final Optional<AccountDTO> account = accountsService.getByExternalId(accountId)
                .map(restMapper::toDTO);

        if (account.isPresent()) {
            context.status(200).json(account.get());
        } else {
            context.status(404)
                    .json(new Error(ErrorCode.ACCOUNT_DOES_NOT_EXIST.getCode(), "Account not found"));
        }
    }

    @Override
    public void getByEmail(final Context context) {
        final String domain = context.pathParam("domain");
        final String email = context.pathParam("email");

        final Optional<AccountDTO> account = accountsService.getByEmail(email, domain)
                .map(restMapper::toDTO);

        if (account.isPresent()) {
            context.status(200).json(account.get());
        } else {
            context.status(404)
                    .json(new Error(ErrorCode.ACCOUNT_DOES_NOT_EXIST.getCode(), "Account not found"));
        }
    }

    @Override
    public void emailExists(final Context context) {
        final String domain = context.pathParam("domain");
        final String email = context.pathParam("email");

        if (!ActorDomainVerifier.verifyActorDomain(context, domain)) {
            return;
        }

        final boolean exists = accountsService.getByEmail(email, domain).isPresent();

        if (exists) {
            context.status(200);
        } else {
            context.status(404);
        }
    }

    @Override
    public void updatePermissions(final Context context) {
        final String accountId = context.pathParam("id");
        final PermissionsRequestDTO request = permissionsRequestBodyHandler.getValidated(context);

        final List<PermissionBO> permissions = request.getPermissions().stream()
                .map(restMapper::toBO)
                .collect(Collectors.toList());

        final AccountDTO updatedAccount;

        if (request.getAction() == PermissionsRequest.Action.GRANT) {
            updatedAccount = restMapper.toDTO(accountsService.grantPermissions(accountId, permissions));
        } else {
            updatedAccount = restMapper.toDTO(accountsService.revokePermissions(accountId, permissions));
        }

        context.json(updatedAccount);
    }

    @Override
    public void updateRoles(final Context context) {
        final String accountId = context.pathParam("id");
        final RolesRequestDTO request = rolesRequestBodyHandler.getValidated(context);

        final AccountDTO updatedAccount;

        if (request.getAction() == RolesRequest.Action.GRANT) {
            updatedAccount = restMapper.toDTO(accountsService.grantRoles(accountId, request.getRoles()));
        } else {
            updatedAccount = restMapper.toDTO(accountsService.revokeRoles(accountId, request.getRoles()));
        }

        context.json(updatedAccount);
    }

    @Override
    public void getApps(final Context context) {
        final String accountId = context.pathParam("id");

        final List<AppDTO> apps = applicationsService.getByAccountId(accountId)
                .stream()
                .map(restMapper::toDTO)
                .collect(Collectors.toList());

        context.status(200).json(apps);
    }

    @Override
    public void activate(final Context context) {
        final String accountId = context.pathParam("id");

        final Optional<AccountDTO> account = accountsService.activate(accountId)
                .map(restMapper::toDTO);

        if (account.isPresent()) {
            context.status(200).json(account.get());
        } else {
            context.status(404).json(new Error("404", "No account with ID " + accountId + " exists"));
        }
    }

    @Override
    public void deactivate(final Context context) {
        final String accountId = context.pathParam("id");

        final Optional<AccountDTO> account = accountsService.deactivate(accountId)
                .map(restMapper::toDTO);

        if (account.isPresent()) {
            context.status(200).json(account.get());
        } else {
            context.status(404).json(new Error("404", "No account with ID " + accountId + " exists"));
        }
    }

    @Override
    public void getActiveLocks(final Context context) {
        final String accountId = context.pathParam("id");

        final Collection<AccountLockDTO> locks = accountLocksService.getActiveLocksByAccountId(accountId)
                .stream()
                .map(restMapper::toDTO)
                .collect(Collectors.toList());

        context.status(200).json(locks);
    }

    private boolean canPerform(final Context context, final CreateAccountRequestDTO request) {
        if (context.attribute("actor") instanceof AppBO) {
            final AppBO actor = context.attribute("actor");
            final boolean isAuthClient = actor.getRoles().contains(AuthGuardRoles.AUTH_CLIENT);

            /*
             * Clients shouldn't have both auth and admin client
             * client roles. If that was the case then it'll still
             * be treated as an auth client and not an admin client.
             */
            return !isAuthClient || canPerform(actor, request);
        }

        return true;
    }

    private boolean canPerform(final AppBO actor, final CreateAccountRequestDTO request) {
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

            final UserIdentifierDTO identifier = request.getIdentifiers().get(0);

            if (identifier.getType() != UserIdentifier.Type.USERNAME) {
                return false;
            }
        }

        return request.getPermissions() == null || request.getPermissions().isEmpty();
    }
}
