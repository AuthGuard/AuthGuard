package com.authguard.service.impl;

import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.service.AccountsService;
import com.authguard.service.VerificationService;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.exceptions.ServiceNotFoundException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.model.AccountBO;
import com.google.inject.Inject;

import java.time.ZonedDateTime;
import java.util.Optional;

public class VerificationServiceImpl implements VerificationService {
    private static final String TARGET_EMAIL_PROPERTY = "email";

    private final AccountTokensRepository accountTokensRepository;
    private final AccountsService accountsService;

    @Inject
    public VerificationServiceImpl(final AccountTokensRepository accountTokensRepository,
                                   final AccountsService accountsService) {
        this.accountTokensRepository = accountTokensRepository;
        this.accountsService = accountsService;
    }


    @Override
    public void verifyEmail(final String verificationToken) {
        final AccountTokenDO accountToken = accountTokensRepository.getByToken(verificationToken)
                .join()
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.TOKEN_EXPIRED_OR_DOES_NOT_EXIST,
                        "AccountDO token " + verificationToken + " does not exist"));

        if (accountToken.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new ServiceException(ErrorCode.EXPIRED_TOKEN, "Token " + verificationToken + " has expired");
        }

        final String verifiedEmail = Optional.ofNullable(accountToken.getAdditionalInformation())
                .map(additional -> additional.get(TARGET_EMAIL_PROPERTY))
                .orElseThrow(() -> new ServiceException(ErrorCode.INVALID_TOKEN, "Invalid account token: no valid additional information"));

        final AccountBO account = accountsService.getById(accountToken.getAssociatedAccountId())
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                        "AccountDO " + accountToken.getAssociatedAccountId() + " does not exist"));

        final AccountBO updated;

        if (verifiedEmail.equals(account.getEmail().getEmail())) {
            updated = account.withEmail(account.getEmail().withVerified(true));
        } else if (verifiedEmail.equals(account.getBackupEmail().getEmail())) {
            updated = account.withBackupEmail(account.getBackupEmail().withVerified(true));
        } else {
            throw new ServiceException(ErrorCode.INVALID_TOKEN, "Account " + account.getId() + " does not contain the " +
                    "email associated with the verification token");
        }

        accountsService.update(updated);
    }
}
