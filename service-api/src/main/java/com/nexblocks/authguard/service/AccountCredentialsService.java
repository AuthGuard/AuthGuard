package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.PasswordResetTokenBO;
import com.nexblocks.authguard.service.model.UserIdentifierBO;

import java.util.List;
import java.util.Optional;

public interface AccountCredentialsService {
    Optional<AccountBO> updatePassword(long id, String plainPassword);
    Optional<AccountBO> addIdentifiers(long id, List<UserIdentifierBO> identifiers);
    Optional<AccountBO> removeIdentifiers(long id, List<String> identifiers);
    Optional<AccountBO> replaceIdentifier(long id, String oldIdentifier, UserIdentifierBO newIdentifier);

    PasswordResetTokenBO generateResetToken(String identifier, boolean returnToken, String domain);
    Optional<AccountBO> resetPasswordByToken(String token, String plainPassword);
    Optional<AccountBO> replacePassword(String identifier, String oldPassword, String newPassword, String domain);
}
