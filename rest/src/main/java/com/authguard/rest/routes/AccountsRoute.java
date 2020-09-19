package com.authguard.rest.routes;

import com.authguard.api.dto.entities.AccountDTO;
import com.authguard.api.dto.entities.AccountEmailDTO;
import com.authguard.api.dto.entities.AppDTO;
import com.authguard.api.dto.requests.*;
import com.authguard.api.routes.AccountsApi;
import com.authguard.rest.exceptions.Error;
import com.authguard.rest.mappers.RestMapper;
import com.authguard.rest.util.BodyHandler;
import com.authguard.rest.util.IdempotencyHeader;
import com.authguard.service.AccountsService;
import com.authguard.service.ApplicationsService;
import com.authguard.service.model.AccountEmailBO;
import com.authguard.service.model.PermissionBO;
import com.authguard.service.model.RequestContextBO;
import com.google.inject.Inject;
import io.javalin.http.Context;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AccountsRoute extends AccountsApi {
    private final AccountsService accountsService;
    private final ApplicationsService applicationsService;
    private final RestMapper restMapper;

    private final BodyHandler<CreateAccountRequestDTO> accountRequestBodyHandler;
    private final BodyHandler<PermissionsRequestDTO> permissionsRequestBodyHandler;
    private final BodyHandler<RolesRequestDTO> rolesRequestBodyHandler;
    private final BodyHandler<AccountEmailsRequestDTO> accountEmailsRequestBodyHandler;

    @Inject
    AccountsRoute(final AccountsService accountsService, final ApplicationsService applicationsService,
                  final RestMapper restMapper) {
        this.accountsService = accountsService;
        this.applicationsService = applicationsService;
        this.restMapper = restMapper;

        this.accountRequestBodyHandler = new BodyHandler.Builder<>(CreateAccountRequestDTO.class)
                .build();
        this.permissionsRequestBodyHandler = new BodyHandler.Builder<>(PermissionsRequestDTO.class)
                .build();
        this.rolesRequestBodyHandler = new BodyHandler.Builder<>(RolesRequestDTO.class)
                .build();
        this.accountEmailsRequestBodyHandler = new BodyHandler.Builder<>(AccountEmailsRequestDTO.class)
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
            context.status(400).result("Failed to create account");
        }
    }

    public void getById(final Context context) {
        final String accountId = context.pathParam("id");

        final Optional<AccountDTO> account = accountsService.getById(accountId)
                .map(restMapper::toDTO);

        if (account.isPresent()) {
            context.status(200).json(account.get());
        } else {
            context.status(404);
        }
    }

    public void deleteAccount(final Context context) {
        final String accountId = context.pathParam("id");

        final Optional<AccountDTO> account = accountsService.delete(accountId)
                .map(restMapper::toDTO);

        if (account.isPresent()) {
            context.status(200).json(account.get());
        } else {
            context.status(404);
        }
    }

    public void getByExternalId(final Context context) {
        final String accountId = context.pathParam("id");

        final Optional<AccountDTO> account = accountsService.getByExternalId(accountId)
                .map(restMapper::toDTO);

        if (account.isPresent()) {
            context.status(200).json(account.get());
        } else {
            context.status(404);
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

    public void addEmails(final Context context) {
        final String accountId = context.pathParam("id");
        final AccountEmailsRequestDTO emailsRequest = accountEmailsRequestBodyHandler.getValidated(context);
        final List<AccountEmailBO> emails = emailsRequest.getEmails().stream()
                .map(restMapper::toBO)
                .collect(Collectors.toList());

        accountsService.addEmails(accountId, emails)
                .map(restMapper::toDTO)
                .ifPresentOrElse(context::json, () -> context.status(404));
    }

    public void removeEmails(final Context context) {
        final String accountId = context.pathParam("id");
        final AccountEmailsRequestDTO emailsRequest = accountEmailsRequestBodyHandler.getValidated(context);
        final List<String> emails = emailsRequest.getEmails().stream()
                .map(AccountEmailDTO::getEmail)
                .collect(Collectors.toList());

        accountsService.removeEmails(accountId, emails)
                .map(restMapper::toDTO)
                .ifPresentOrElse(context::json, () -> context.status(404));
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
}
