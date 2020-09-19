package com.authguard.service;

import com.authguard.service.exceptions.ServiceNotFoundException;
import com.authguard.service.impl.AccountsServiceImpl;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AccountEmailBO;
import com.authguard.service.model.PermissionBO;
import com.authguard.service.model.RequestContextBO;

import java.util.List;
import java.util.Optional;

/**
 * AccountDO service interface.
 *
 * @see AccountsServiceImpl
 */
public interface AccountsService {
    /**
     * Create an account. The returned object of this method
     * is not necessarily the same as the argument. Fields
     * like 'id' and 'deleted' will be overwritten by any
     * implementation.
     */
    AccountBO create(AccountBO account, RequestContextBO requestContext);

    /**
     * Find an account by ID.
     */
    Optional<AccountBO> getById(String accountId);

    Optional<AccountBO> getByExternalId(String externalId);

    Optional<AccountBO> update(AccountBO account);

    Optional<AccountBO> delete(String accountId);

    Optional<AccountBO> activate(String accountId);

    Optional<AccountBO> deactivate(String accountId);

    /**
     * Get all permissions of an account. Implementations of
     * this method should aggregate permissions granted to the
     * account directly and permissions granted by roles and
     * scopes.
     * @throws ServiceNotFoundException
     *         if no account was found.
     */
    List<PermissionBO> getPermissions(String accountId);

    Optional<AccountBO> removeEmails(String accountId, List<String> emails);

    Optional<AccountBO> addEmails(String accountId, List<AccountEmailBO> emails);

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
}
