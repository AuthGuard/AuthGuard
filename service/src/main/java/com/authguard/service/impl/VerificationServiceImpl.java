package com.authguard.service.impl;

import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.service.AccountsService;
import com.authguard.service.VerificationService;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.exceptions.ServiceNotFoundException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AccountEmailBO;
import com.google.inject.Inject;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class VerificationServiceImpl implements VerificationService {
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

        if (accountToken.getAdditionalInformation() == null
                || !(accountToken.getAdditionalInformation() instanceof String)) {
            throw new ServiceException(ErrorCode.INVALID_TOKEN, "Invalid account token: no valid additional information");
        }

        if (accountToken.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new ServiceException(ErrorCode.EXPIRED_TOKEN, "Token " + verificationToken + " has expired");
        }

        final String verifiedEmail = (String) accountToken.getAdditionalInformation();
        final AccountBO account = accountsService.getById(accountToken.getAssociatedAccountId())
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                        "AccountDO " + accountToken.getAssociatedAccountId() + " does not exist"));

        final List<AccountEmailBO> updatedEmails = new ArrayList<>();
        boolean emailFound = false;

        for (final AccountEmailBO email : account.getEmails()) {
            if (email.getEmail().equals(verifiedEmail)) {
                updatedEmails.add(AccountEmailBO.builder().from(email).verified(true).build());
                emailFound = true;
            } else {
                updatedEmails.add(email);
            }
        }

        if (!emailFound) {
            throw new ServiceException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                    "AccountDO " + account.getId() + " does not contain email " + verifiedEmail);
        }

        final AccountBO updated = AccountBO.builder().from(account)
                .emails(updatedEmails)
                .build();

        accountsService.update(updated);
    }
}
