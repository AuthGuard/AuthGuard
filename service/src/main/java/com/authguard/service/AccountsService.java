package com.authguard.service;

import com.authguard.service.exceptions.ServiceNotFoundException;
import com.authguard.service.impl.AccountsServiceImpl;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.PermissionBO;

import java.util.List;
import java.util.Optional;

/**
 * Account service interface.
 *
 * @see AccountsServiceImpl
 */
public interface AccountsService {
    /**
     * Create an account. The returned object of this method
     * is not necessarily the same as the argument. Fields
     * like 'id' and 'deleted' will be overwritten by any
     * implementation.
     * @param account An account to be created.
     * @return The stored account in the repository.
     */
    AccountBO create(AccountBO account);

    /**
     * Find an account by ID.
     * @param accountId The ID of the account. This is different
     *                  from credentials ID
     * @return An optional of containing the account object, or
     *         empty if none was found.
     */
    Optional<AccountBO> getById(String accountId);

    /**
     * Get all permissions of an account. Implementations of
     * this method should aggregate permissions granted to the
     * account directly and permissions granted by roles and
     * scopes.
     * @param accountId The ID of the account. This is different
     *                  from credentials ID
     * @return A list of all permissions the account has
     * @throws ServiceNotFoundException
     *         if no account was found.
     */
    List<PermissionBO> getPermissions(String accountId);

    /**
     * Grant permissions to an account. This should only update
     * the permissions field.
     * @param accountId The ID of the account. This is different
     *                  from credentials ID
     * @param permissions A list of permissions to grant.
     * @return The updated account
     * @throws ServiceNotFoundException
     *         if no account was found.
     */
    AccountBO grantPermissions(String accountId, List<PermissionBO> permissions);

    /**
     * Revoke permissions of an account. This should only update
     * the permissions field.
     * @param accountId The ID of the account. This is different
     *                  from credentials ID
     * @param permissions A list of permissions to revoke.
     * @return The updated account
     * @throws ServiceNotFoundException
     *         if no account was found.
     */
    AccountBO revokePermissions(String accountId, List<PermissionBO> permissions);

    /**
     * Grant roles to an account. This should only update the roles
     * field.
     * @param accountId The ID of the account. This is different
     *                  from credentials ID
     * @param roles A list of roles to grant.
     * @return The updated account
     * @throws ServiceNotFoundException
     *         if no account was found.
     */
    AccountBO grantRoles(String accountId, List<String> roles);

    /**
     * Revoke roles of an account. This should only update the roles
     * field.
     * @param accountId The ID of the account. This is different
     *                  from credentials ID
     * @param roles A list of roles to revoke.
     * @return The updated account
     * @throws ServiceNotFoundException
     *         if no account was found.
     */
    AccountBO revokeRoles(String accountId, List<String> roles);

    /**
     * Finds a list of all admins. This is useful only when deciding
     * if a one-time admin account should be created or not.
     * @return
     */
    List<AccountBO> getAdmins();
}
