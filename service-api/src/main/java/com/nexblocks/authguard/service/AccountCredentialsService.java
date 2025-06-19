package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.PasswordResetTokenBO;
import com.nexblocks.authguard.service.model.UserIdentifierBO;

import io.smallrye.mutiny.Uni;

public interface AccountCredentialsService {
    Uni<AccountBO> updatePassword(long id, String plainPassword, String domain);
    Uni<AccountBO> addIdentifiers(long id, UserIdentifierBO identifier, String domain);
    Uni<AccountBO> removeIdentifiers(long id, String identifier, String domain);
    Uni<AccountBO> replaceIdentifier(long id, String oldIdentifier, UserIdentifierBO newIdentifier, String domain);

    Uni<PasswordResetTokenBO> generateResetToken(String identifier, boolean returnToken, String domain);
    Uni<AccountBO> resetPasswordByToken(String token, String plainPassword, String domain);
    Uni<AccountBO> replacePassword(String identifier, String oldPassword, String newPassword, String domain);
}
