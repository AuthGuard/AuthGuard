package org.auther.api;

import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.auther.api.dto.AccountDTO;
import org.auther.api.dto.PermissionsRequestDTO;
import org.auther.service.AccountsService;
import org.auther.service.model.PermissionBO;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.post;

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
        post("/:id/permission/grant", this::grantPermissions);
        post("/:id/permission/revoke", this::revokePermissions);
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
