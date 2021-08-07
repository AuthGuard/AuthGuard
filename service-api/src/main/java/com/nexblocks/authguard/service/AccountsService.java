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

    Optional<AccountBO> getByExternalId(String externalId);

    Optional<AccountBO> getByEmail(String email);

    Optional<AccountBO> activate(String accountId);

    Optional<AccountBO> deactivate(String accountId);

    Optional<AccountBO> patch(String accountId, AccountBO account);

    /**
     * Grant permissions to an account. This should only updatePatch
     * the permissions field.
     * @throws ServiceNotFoundException
     *         if no account was found.
     */
    AccountBO grantPermissions(String accountId, List<PermissionBO> permissions);

    /**
     * Revoke permissions of an account. This should only updatePatch
     * the permissions field.
     * @throws ServiceNotFoundException
     *         if no account was found.
     */
    AccountBO revokePermissions(String accountId, List<PermissionBO> permissions);

    /**
     * Grant roles to an account. This should only updatePatch the roles
     * field.
     * @throws ServiceNotFoundException
     *         if no account was found.
     */
    AccountBO grantRoles(String accountId, List<String> roles);

    /**
     * Revoke roles of an account. This should only updatePatch the roles
     * field.
     * @throws ServiceNotFoundException
     *         if no account was found.
     */
    AccountBO revokeRoles(String accountId, List<String> roles);

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
    List<AccountBO> getByRole(String role);
}
