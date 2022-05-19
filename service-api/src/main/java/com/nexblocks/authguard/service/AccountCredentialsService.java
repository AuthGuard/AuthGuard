package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.PasswordResetTokenBO;
import com.nexblocks.authguard.service.model.UserIdentifierBO;

import java.util.List;
import java.util.Optional;

public interface AccountCredentialsService {
    Optional<AccountBO> updatePassword(String id, String plainPassword);
    Optional<AccountBO> addIdentifiers(String id, List<UserIdentifierBO> identifiers);
    Optional<AccountBO> removeIdentifiers(String id, List<String> identifiers);
    Optional<AccountBO> replaceIdentifier(String id, String oldIdentifier, UserIdentifierBO newIdentifier);

    PasswordResetTokenBO generateResetToken(String identifier, boolean returnToken, String domain);
    Optional<AccountBO> resetPasswordByToken(String token, String plainPassword);
    Optional<AccountBO> replacePassword(String identifier, String oldPassword, String newPassword, String domain);
}
