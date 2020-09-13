package com.authguard.rest.routes;

import com.authguard.api.dto.entities.AccountDTO;
import com.authguard.api.dto.entities.AccountEmailDTO;
import com.authguard.api.dto.entities.AppDTO;
import com.authguard.api.dto.requests.*;
import com.authguard.rest.access.ActorRoles;
import com.authguard.rest.exceptions.Error;
import com.authguard.rest.util.BodyHandler;
import com.authguard.service.AccountsService;
import com.authguard.service.ApplicationsService;
import com.authguard.service.model.AccountEmailBO;
import com.authguard.service.model.PermissionBO;
import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.*;

public class AccountsRoute implements EndpointGroup {
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

    public void addEndpoints() {
        post("/", this::create, ActorRoles.of("authguard_admin_client", "one_time_admin"));

        get("/:id", this::getById, ActorRoles.adminClient());
        delete("/:id", this::deleteAccount, ActorRoles.adminClient());
        get("/externalId/:id", this::getByExternalId, ActorRoles.adminClient());

        patch("/:id/permissions", this::updatePermissions, ActorRoles.adminClient());
        patch("/:id/roles", this::updateRoles, ActorRoles.adminClient());

        patch("/:id/emails", this::addEmails, ActorRoles.adminClient());
        delete("/:id/emails", this::removeEmails, ActorRoles.adminClient());

        get("/:id/apps", this::getApps, ActorRoles.adminClient());

        patch("/:id/activate", this::activate, ActorRoles.adminClient());
        patch("/:id/deactivate", this::deactivate, ActorRoles.adminClient());
    }

    private void create(final Context context) {
        final CreateAccountRequestDTO request = accountRequestBodyHandler.getValidated(context);

        final Optional<AccountDTO> createdAccount = Optional.of(restMapper.toBO(request))
                .map(accountsService::create)
                .map(restMapper::toDTO);

        if (createdAccount.isPresent()) {
            context.status(201).json(createdAccount.get());
        } else {
            context.status(400).result("Failed to create account");
        }
    }

    private void getById(final Context context) {
        final String accountId = context.pathParam("id");

        final Optional<AccountDTO> account = accountsService.getById(accountId)
                .map(restMapper::toDTO);

        if (account.isPresent()) {
            context.status(200).json(account.get());
        } else {
            context.status(404);
        }
    }

    private void deleteAccount(final Context context) {
        final String accountId = context.pathParam("id");

        final Optional<AccountDTO> account = accountsService.delete(accountId)
                .map(restMapper::toDTO);

        if (account.isPresent()) {
            context.status(200).json(account.get());
        } else {
            context.status(404);
        }
    }

    private void getByExternalId(final Context context) {
        final String accountId = context.pathParam("id");

        final Optional<AccountDTO> account = accountsService.getByExternalId(accountId)
                .map(restMapper::toDTO);

        if (account.isPresent()) {
            context.status(200).json(account.get());
        } else {
            context.status(404);
        }
    }

    private void updatePermissions(final Context context) {
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

    private void updateRoles(final Context context) {
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

    private void addEmails(final Context context) {
        final String accountId = context.pathParam("id");
        final AccountEmailsRequestDTO emailsRequest = accountEmailsRequestBodyHandler.getValidated(context);
        final List<AccountEmailBO> emails = emailsRequest.getEmails().stream()
                .map(restMapper::toBO)
                .collect(Collectors.toList());

        accountsService.addEmails(accountId, emails)
                .map(restMapper::toDTO)
                .ifPresentOrElse(context::json, () -> context.status(404));
    }

    private void removeEmails(final Context context) {
        final String accountId = context.pathParam("id");
        final AccountEmailsRequestDTO emailsRequest = accountEmailsRequestBodyHandler.getValidated(context);
        final List<String> emails = emailsRequest.getEmails().stream()
                .map(AccountEmailDTO::getEmail)
                .collect(Collectors.toList());

        accountsService.removeEmails(accountId, emails)
                .map(restMapper::toDTO)
                .ifPresentOrElse(context::json, () -> context.status(404));
    }

    private void getApps(final Context context) {
        final String accountId = context.pathParam("id");

        final List<AppDTO> apps = applicationsService.getByAccountId(accountId)
                .stream()
                .map(restMapper::toDTO)
                .collect(Collectors.toList());

        context.status(200).json(apps);
    }

    private void activate(final Context context) {
        final String accountId = context.pathParam("id");

        final Optional<AccountDTO> account = accountsService.activate(accountId)
                .map(restMapper::toDTO);

        if (account.isPresent()) {
            context.status(200).json(account.get());
        } else {
            context.status(404).json(new Error("404", "No account with ID " + accountId + " exists"));
        }
    }

    private void deactivate(final Context context) {
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
