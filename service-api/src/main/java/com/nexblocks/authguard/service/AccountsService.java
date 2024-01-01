package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.PermissionBO;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * AccountDO service interface.
 */
public interface AccountsService extends IdempotentCrudService<AccountBO> {
    CompletableFuture<Optional<AccountBO>> getByIdUnsafe(long id);

    CompletableFuture<Optional<AccountBO>> getByExternalId(String externalId);

    CompletableFuture<Optional<AccountBO>> getByEmail(String email, String domain);

    CompletableFuture<Optional<AccountBO>> getByIdentifier(String identifier, String domain);
    CompletableFuture<Optional<AccountBO>> getByIdentifierUnsafe(String identifier, String domain);

    CompletableFuture<Optional<AccountBO>> activate(long accountId);

    CompletableFuture<Optional<AccountBO>> deactivate(long accountId);

    CompletableFuture<Optional<AccountBO>> patch(long accountId, AccountBO account);

    /**
     * Grant permissions to an account. This should only updatePatch
     * the permissions field.
     *
     * @throws ServiceNotFoundException if no account was found.
     */
    CompletableFuture<Optional<AccountBO>> grantPermissions(long accountId, List<PermissionBO> permissions);

    /**
     * Revoke permissions of an account. This should only updatePatch
     * the permissions field.
     *
     * @throws ServiceNotFoundException if no account was found.
     */
    CompletableFuture<Optional<AccountBO>> revokePermissions(long accountId, List<PermissionBO> permissions);

    /**
     * Grant roles to an account. This should only updatePatch the roles
     * field.
     *
     * @throws ServiceNotFoundException if no account was found.
     */
    CompletableFuture<Optional<AccountBO>> grantRoles(long accountId, List<String> roles);

    /**
     * Revoke roles of an account. This should only updatePatch the roles
     * field.
     *
     * @throws ServiceNotFoundException if no account was found.
     */
    CompletableFuture<Optional<AccountBO>> revokeRoles(long accountId, List<String> roles);

    /**
     * Finds a list of all admins. This is useful only when deciding
     * if a one-time admin account should be created or not.
     */
    CompletableFuture<List<AccountBO>> getAdmins();

    /**
     * Finds a list of all accounts with a certain role.
     * This is useful only when deciding if a one-time
     * admin account should be created or not.
     */
    CompletableFuture<List<AccountBO>> getByRole(String role, String domain);
}
