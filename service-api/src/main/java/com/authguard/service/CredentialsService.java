package com.authguard.service;

import com.authguard.service.model.CredentialsBO;
import com.authguard.service.model.RequestContextBO;
import com.authguard.service.model.UserIdentifierBO;

import java.util.List;
import java.util.Optional;

/**
 * CredentialsDO service interface.
 */
public interface CredentialsService {
    /**
     * Create credentials. The ID of the returned object
     * is not necessarily the same as that of the argument.
     * @param credentials The credentials to create
     * @return The created credentials in the repository.
     */
    CredentialsBO create(CredentialsBO credentials, RequestContextBO requestContext);

    /**
     * Find credentials by ID.
     * @param id The ID of the credentials. This is
     *           different from the account ID.
     * @return An optional of the retrieved credentials
     *         or empty if none was found.
     */
    Optional<CredentialsBO> getById(String id);

    /**
     * Find credentials by username.
     * @param username The username of the credentials.
     * @return An optional of the retrieved credentials
     *         or empty if none was found.
     */
    Optional<CredentialsBO> getByUsername(String username);

    /**
     * Find credentials by username.
     * @param username The username of the credentials.
     * @return An optional of the retrieved credentials
     *         or empty if none was found.
     */
    Optional<CredentialsBO> getByUsernameUnsafe(String username);

    /**
     * Update any updatable field other than the password.
     * @param credentials The credentials object to update.
     * @return An optional of the updated credentials or
     *         empty if none was found to update.
     */
    Optional<CredentialsBO> update(CredentialsBO credentials);

    /**
     * Update the password in a credentials object.
     * @param credentials The credentials object to update.
     * @return An optional of the updated credentials or
     *         empty if none was found to update.
     */
    Optional<CredentialsBO> updatePassword(CredentialsBO credentials);

    Optional<CredentialsBO> addIdentifiers(String id, List<UserIdentifierBO> identifiers);

    Optional<CredentialsBO> removeIdentifiers(String id, List<String> identifiers);

    /**
     * Delete credentials. Whether it is a soft or a hard
     * delete depends on the implementation.
     * @param id The ID of the credentials. This is different
     *           from account ID.
     * @return An optional of the deleted credentials or
     *         empty if non was found.
     */
    Optional<CredentialsBO> delete(String id);
}
