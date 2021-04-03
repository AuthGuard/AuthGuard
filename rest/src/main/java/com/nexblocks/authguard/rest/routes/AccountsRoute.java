package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.dto.entities.AccountDTO;
import com.nexblocks.authguard.api.dto.entities.AccountLockDTO;
import com.nexblocks.authguard.api.dto.entities.AppDTO;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.api.dto.requests.*;
import com.nexblocks.authguard.api.routes.AccountsApi;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.util.BodyHandler;
import com.nexblocks.authguard.rest.util.IdempotencyHeader;
import com.nexblocks.authguard.service.AccountLocksService;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.ApplicationsService;
import com.nexblocks.authguard.service.CredentialsService;
import com.nexblocks.authguard.service.exceptions.IdempotencyException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.CredentialsBO;
import com.nexblocks.authguard.service.model.PermissionBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import io.javalin.http.Context;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

public class AccountsRoute extends AccountsApi {
    private final AccountsService accountsService;
    private final CredentialsService credentialsService;
    private final ApplicationsService applicationsService;
    private final AccountLocksService accountLocksService;
    private final RestMapper restMapper;

    private final BodyHandler<CreateAccountRequestDTO> accountRequestBodyHandler;
    private final BodyHandler<UpdateAccountRequestDTO> updateAccountRequestBodyHandler;
    private final BodyHandler<CreateCompleteAccountRequestDTO> completeAccountRequestBodyHandler;
    private final BodyHandler<PermissionsRequestDTO> permissionsRequestBodyHandler;
    private final BodyHandler<RolesRequestDTO> rolesRequestBodyHandler;

    @Inject
    AccountsRoute(final AccountsService accountsService,
                  final CredentialsService credentialsService,
                  final ApplicationsService applicationsService,
                  final AccountLocksService accountLocksService,
                  final RestMapper restMapper) {
        this.accountsService = accountsService;
        this.credentialsService = credentialsService;
        this.applicationsService = applicationsService;
        this.accountLocksService = accountLocksService;
        this.restMapper = restMapper;

        this.accountRequestBodyHandler = new BodyHandler.Builder<>(CreateAccountRequestDTO.class)
                .build();
        this.updateAccountRequestBodyHandler = new BodyHandler.Builder<>(UpdateAccountRequestDTO.class)
                .build();
        this.completeAccountRequestBodyHandler = new BodyHandler.Builder<>(CreateCompleteAccountRequestDTO.class)
                .build();
        this.permissionsRequestBodyHandler = new BodyHandler.Builder<>(PermissionsRequestDTO.class)
                .build();
        this.rolesRequestBodyHandler = new BodyHandler.Builder<>(RolesRequestDTO.class)
                .build();
    }

    public void create(final Context context) {
        final String idempotentKey = IdempotencyHeader.getKeyOrFail(context);
        final CreateAccountRequestDTO request = accountRequestBodyHandler.getValidated(context);

        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .source(context.ip())
                .build();

        final Optional<AccountDTO> createdAccount = Optional.of(restMapper.toBO(request))
                .map(accountBO -> accountsService.create(accountBO, requestContext))
                .map(restMapper::toDTO);

        if (createdAccount.isPresent()) {
            context.status(201).json(createdAccount.get());
        } else {
            context.status(400).json(new Error("400", "Failed to create account"));
        }
    }

    @Override
    public void createComplete(final Context context) {
        final String idempotentKey = IdempotencyHeader.getKeyOrFail(context);
        final CreateCompleteAccountRequestDTO request = completeAccountRequestBodyHandler.getValidated(context);

        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .source(context.ip())
                .build();

        final AccountBO accountBO = restMapper.toBO(request.getAccount());
        final CredentialsBO credentialsBO = restMapper.toBO(request.getCredentials());

        String accountId;
        String credentialsId;

        try {
            accountId = accountsService.create(accountBO, requestContext).getId();
        } catch (final CompletionException e) {
            if (e.getCause() instanceof IdempotencyException) {
                accountId = ((IdempotencyException) e.getCause()).getIdempotentRecord().getEntityId();
            } else {
                throw e;
            }
        }

        try {
            credentialsId = credentialsService.create(credentialsBO.withAccountId(accountId), requestContext).getId();
        } catch (final CompletionException e) {
            if (e.getCause() instanceof IdempotencyException) {
                credentialsId = ((IdempotencyException) e.getCause()).getIdempotentRecord().getEntityId();
            } else {
                throw e;
            }
        }

        final CreateCompleteAccountResponseDTO response = CreateCompleteAccountResponseDTO.builder()
                .accountId(accountId)
                .credentialsId(credentialsId)
                .build();

        context.status(201).json(response);
    }

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
        final String email = context.pathParam("email");

        final Optional<AccountDTO> account = accountsService.getByEmail(email)
                .map(restMapper::toDTO);

        if (account.isPresent()) {
            context.status(200).json(account.get());
        } else {
            context.status(404)
                    .json(new Error(ErrorCode.ACCOUNT_DOES_NOT_EXIST.getCode(), "Account not found"));
        }
    }

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

    public void getApps(final Context context) {
        final String accountId = context.pathParam("id");

        final List<AppDTO> apps = applicationsService.getByAccountId(accountId)
                .stream()
                .map(restMapper::toDTO)
                .collect(Collectors.toList());

        context.status(200).json(apps);
    }

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
}
