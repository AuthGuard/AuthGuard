package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.CredentialsBO;
import com.nexblocks.authguard.service.model.UserIdentifierBO;

import java.util.List;
import java.util.Optional;

/**
 * CredentialsDO service interface.
 */
public interface CredentialsService extends IdempotentCrudService<CredentialsBO> {

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
     * Update the password in a credentials object.
     * @param credentials The credentials object to update.
     * @return An optional of the updated credentials or
     *         empty if none was found to update.
     */
    Optional<CredentialsBO> updatePassword(CredentialsBO credentials);

    Optional<CredentialsBO> addIdentifiers(String id, List<UserIdentifierBO> identifiers);

    Optional<CredentialsBO> removeIdentifiers(String id, List<String> identifiers);
}
