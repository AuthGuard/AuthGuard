package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.CredentialsBO;
import com.nexblocks.authguard.service.model.PasswordResetTokenBO;
import com.nexblocks.authguard.service.model.UserIdentifierBO;

import java.util.List;
import java.util.Optional;

public interface CredentialsService extends IdempotentCrudService<CredentialsBO> {
    Optional<CredentialsBO> getByIdUnsafe(String id);
    Optional<CredentialsBO> getByUsername(String username);
    Optional<CredentialsBO> getByUsernameUnsafe(String username);

    Optional<CredentialsBO> updatePassword(String id, String plainPassword);
    Optional<CredentialsBO> addIdentifiers(String id, List<UserIdentifierBO> identifiers);
    Optional<CredentialsBO> removeIdentifiers(String id, List<String> identifiers);
    Optional<CredentialsBO> replaceIdentifier(String id, String oldIdentifier, UserIdentifierBO newIdentifier);

    PasswordResetTokenBO generateResetToken(String identifier, boolean returnToken);
    Optional<CredentialsBO> resetPasswordByToken(String token, String plainPassword);
    Optional<CredentialsBO> replacePassword(String identifier, String oldPassword, String newPassword);
}
