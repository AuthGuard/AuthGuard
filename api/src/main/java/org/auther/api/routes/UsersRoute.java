package org.auther.api.routes;

import com.auther.config.ConfigContext;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.auther.api.dto.*;
import org.auther.service.AccountsService;
import org.auther.service.model.PermissionBO;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.get;

public class UsersRoute implements EndpointGroup {
    private final AccountsService accountsService;
    private final RestMapper restMapper;
    private final ConfigContext configContext;

    public UsersRoute(final AccountsService accountsService, final ConfigContext configContext) {
        this.accountsService = accountsService;
        this.configContext = configContext;
        restMapper = RestMapper.INSTANCE;
    }

    public void addEndpoints() {
        get("/", this::index);
        post("/", this::create);
        post("/authenticate", this::authenticate);
        post("/:id/permission/grant", this::grantPermissions);
        post("/:id/permission/revoke", this::revokePermissions);
    }

    private void index(final Context context){
        context.status(200).json(this.configContext.get("jwt"));
    }

    private void create(final Context context) {
        final AccountDTO account = context.bodyAsClass(AccountDTO.class);

        final Optional<AccountDTO> createdAccount = Optional.of(restMapper.toBO(account))
                .map(accountsService::create)
                .map(restMapper::toDTO);

        if (createdAccount.isPresent()) {
            context.json(createdAccount.get());
        } else {
            context.status(400).result("Failed to create account");
        }
    }

    private void authenticate(final Context context) {
        final AuthenticationRequestDTO authenticationRequest = context.bodyAsClass(AuthenticationRequestDTO.class);

        final Optional<TokensDTO> tokens = accountsService.authenticate(authenticationRequest.getAuthorization())
                .map(restMapper::toDTO);

        if (tokens.isPresent()) {
            context.json(tokens.get());
        } else {
            context.status(400).result("Failed to authenticate user");
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
