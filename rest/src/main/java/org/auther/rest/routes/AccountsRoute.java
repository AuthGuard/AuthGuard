package org.auther.rest.routes;

import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.auther.rest.access.ActorRole;
import org.auther.rest.dto.AccountDTO;
import org.auther.rest.dto.PermissionsRequestDTO;
import org.auther.service.AccountsService;
import org.auther.service.model.PermissionBO;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.core.security.SecurityUtil.roles;

public class AccountsRoute implements EndpointGroup {
    private final AccountsService accountsService;
    private final RestMapper restMapper;

    @Inject
    AccountsRoute(final AccountsService accountsService, final RestMapper restMapper) {
        this.accountsService = accountsService;
        this.restMapper = restMapper;
    }

    public void addEndpoints() {
        post("/", this::create);
        get("/:id", this::getById);
        post("/:id/permission/grant", this::grantPermissions, roles(ActorRole.of("admin")));
        post("/:id/permission/revoke", this::revokePermissions, roles(ActorRole.of("admin")));
    }

    private void create(final Context context) {
        final AccountDTO account = context.bodyAsClass(AccountDTO.class);

        final Optional<AccountDTO> createdAccount = Optional.of(restMapper.toBO(account))
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

    private void grantPermissions(final Context context) {
        final String accountId = context.pathParam("id");
        final PermissionsRequestDTO permissionsRequest = context.bodyAsClass(PermissionsRequestDTO.class);
        final List<PermissionBO> permissions = permissionsRequest.getPermissions().stream()
                .map(restMapper::toBO)
                .collect(Collectors.toList());

        final AccountDTO updatedAccount = restMapper.toDTO(accountsService.grantPermissions(accountId, permissions));
        context.json(updatedAccount);
    }

    private void revokePermissions(final Context context) {
        final String accountId = context.pathParam("id");
        final PermissionsRequestDTO permissionsRequest = context.bodyAsClass(PermissionsRequestDTO.class);
        final List<PermissionBO> permissions = permissionsRequest.getPermissions().stream()
                .map(restMapper::toBO)
                .collect(Collectors.toList());

        final AccountDTO updatedAccount = restMapper.toDTO(accountsService.revokePermissions(accountId, permissions));
        context.json(updatedAccount);
    }
}
