package com.authguard.service.impl;

import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.service.AccountsService;
import com.authguard.service.VerificationService;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.exceptions.ServiceNotFoundException;
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
                .orElseThrow(() -> new ServiceNotFoundException("Account token " + verificationToken + " does not exist"));

        if (accountToken.getAdditionalInformation() == null
                || !(accountToken.getAdditionalInformation() instanceof String)) {
            throw new ServiceException("Invalid account token: no valid additional information");
        }

        if (accountToken.expiresAt().isBefore(ZonedDateTime.now())) {
            throw new ServiceException("Token " + verificationToken + " has expired");
        }

        final String verifiedEmail = (String) accountToken.getAdditionalInformation();
        final AccountBO account = accountsService.getById(accountToken.getAssociatedAccountId())
                .orElseThrow(() -> new ServiceNotFoundException("Account " + accountToken.getAssociatedAccountId() + " does not exist"));

        final List<AccountEmailBO> updatedEmails = new ArrayList<>();
        boolean emailFound = false;

        for (final AccountEmailBO email : account.getAccountEmails()) {
            if (email.getEmail().equals(verifiedEmail)) {
                updatedEmails.add(AccountEmailBO.builder().from(email).verified(true).build());
                emailFound = true;
            } else {
                updatedEmails.add(email);
            }
        }

        if (!emailFound) {
            throw new ServiceException("Account " + account.getId() + " does not contain email " + verifiedEmail);
        }

        final AccountBO updated = AccountBO.builder().from(account)
                .accountEmails(updatedEmails)
                .build();

        accountsService.update(updated);
    }
}
