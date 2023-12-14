package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.PermissionBO;

import java.util.List;
import java.util.Optional;

/**
 * AccountDO service interface.
 */
public interface AccountsService extends IdempotentCrudService<AccountBO> {
    Optional<AccountBO> getByIdUnsafe(long id);

    Optional<AccountBO> getByExternalId(String externalId);

    Optional<AccountBO> getByEmail(String email, String domain);

    Optional<AccountBO> getByIdentifier(String identifier, String domain);
    Optional<AccountBO> getByIdentifierUnsafe(String identifier, String domain);

    Optional<AccountBO> activate(long accountId);

    Optional<AccountBO> deactivate(long accountId);

    Optional<AccountBO> patch(long accountId, AccountBO account);

    /**
     * Grant permissions to an account. This should only updatePatch
     * the permissions field.
     * @throws ServiceNotFoundException
     *         if no account was found.
     */
    Optional<AccountBO> grantPermissions(long accountId, List<PermissionBO> permissions);

    /**
     * Revoke permissions of an account. This should only updatePatch
     * the permissions field.
     * @throws ServiceNotFoundException
     *         if no account was found.
     */
    Optional<AccountBO> revokePermissions(long accountId, List<PermissionBO> permissions);

    /**
     * Grant roles to an account. This should only updatePatch the roles
     * field.
     * @throws ServiceNotFoundException
     *         if no account was found.
     */
    Optional<AccountBO> grantRoles(long accountId, List<String> roles);

    /**
     * Revoke roles of an account. This should only updatePatch the roles
     * field.
     * @throws ServiceNotFoundException
     *         if no account was found.
     */
    Optional<AccountBO> revokeRoles(long accountId, List<String> roles);

    /**
     * Finds a list of all admins. This is useful only when deciding
     * if a one-time admin account should be created or not.
     */
    List<AccountBO> getAdmins();

    /**
     * Finds a list of all accounts with a certain role.
     * This is useful only when deciding if a one-time
     * admin account should be created or not.
     */
    List<AccountBO> getByRole(String role, String domain);
}
