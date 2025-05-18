package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.PermissionBO;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AccountsService extends IdempotentCrudService<AccountBO> {
    CompletableFuture<AccountBO> getByIdUnsafe(long id, String domain);

    CompletableFuture<Optional<AccountBO>> getByIdUnchecked(long id);

    CompletableFuture<Optional<AccountBO>> getByExternalId(String externalId, String domain);

    CompletableFuture<Optional<AccountBO>> getByExternalIdUnchecked(String externalId);

    CompletableFuture<Optional<AccountBO>> getByEmail(String email, String domain);

    CompletableFuture<Optional<AccountBO>> getByIdentifier(String identifier, String domain);
    CompletableFuture<Optional<AccountBO>> getByIdentifierUnsafe(String identifier, String domain);

    CompletableFuture<Optional<AccountBO>> activate(long accountId, String domain);

    CompletableFuture<Optional<AccountBO>> deactivate(long accountId, String domain);

    CompletableFuture<Optional<AccountBO>> patch(long accountId, AccountBO account, String domain);

    /**
     * Grant permissions to an account. This should only updatePatch
     * the permissions field.
     *
     * @throws ServiceNotFoundException if no account was found.
     */
    CompletableFuture<Optional<AccountBO>> grantPermissions(long accountId, List<PermissionBO> permissions, String domain);

    /**
     * Revoke permissions of an account. This should only updatePatch
     * the permissions field.
     *
     * @throws ServiceNotFoundException if no account was found.
     */
    CompletableFuture<Optional<AccountBO>> revokePermissions(long accountId, List<PermissionBO> permissions, String domain);

    /**
     * Grant roles to an account. This should only updatePatch the roles
     * field.
     *
     * @throws ServiceNotFoundException if no account was found.
     */
    CompletableFuture<Optional<AccountBO>> grantRoles(long accountId, List<String> roles, String domain);

    /**
     * Revoke roles of an account. This should only updatePatch the roles
     * field.
     *
     * @throws ServiceNotFoundException if no account was found.
     */
    CompletableFuture<Optional<AccountBO>> revokeRoles(long accountId, List<String> roles, String domain);

    /**
     * Finds a list of all admins. This is useful only when deciding
     * if a one-time admin account should be created or not.
     */
    Uni<List<AccountBO>> getAdmins();

    /**
     * Finds a list of all accounts with a certain role.
     * This is useful only when deciding if a one-time
     * admin account should be created or not.
     */
    Uni<List<AccountBO>> getByRole(String role, String domain);
}
