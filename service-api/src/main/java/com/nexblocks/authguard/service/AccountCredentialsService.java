package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.PasswordResetTokenBO;
import com.nexblocks.authguard.service.model.UserIdentifierBO;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AccountCredentialsService {
    CompletableFuture<AccountBO> updatePassword(long id, String plainPassword);
    CompletableFuture<AccountBO> addIdentifiers(long id, List<UserIdentifierBO> identifiers);
    CompletableFuture<AccountBO> removeIdentifiers(long id, List<String> identifiers);
    CompletableFuture<AccountBO> replaceIdentifier(long id, String oldIdentifier, UserIdentifierBO newIdentifier);

    CompletableFuture<PasswordResetTokenBO> generateResetToken(String identifier, boolean returnToken, String domain);
    CompletableFuture<AccountBO> resetPasswordByToken(String token, String plainPassword);
    CompletableFuture<AccountBO> replacePassword(String identifier, String oldPassword, String newPassword, String domain);
}
