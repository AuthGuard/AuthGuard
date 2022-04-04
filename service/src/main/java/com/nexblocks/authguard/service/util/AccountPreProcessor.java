package com.nexblocks.authguard.service.util;

import com.nexblocks.authguard.service.config.AccountConfig;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AccountBO;

import java.util.Set;

public class AccountPreProcessor {
    public static AccountBO preProcess(final AccountBO account, final AccountConfig accountConfig) {
        if (account.getEmail() == null && accountConfig.requireEmail()) {
            throw new ServiceException(ErrorCode.ACCOUNT_EMAIL_REQUIRED, "Account must have an email");
        }

        if (account.getPhoneNumber() == null && accountConfig.requirePhoneNumber()) {
            throw new ServiceException(ErrorCode.ACCOUNT_PHONE_NUMBER_REQUIRED, "Account must have a phone number");
        }

        final boolean hasNoRoles = account.getRoles() == null || account.getRoles().isEmpty();
        final Set<String> defaultRoles = accountConfig.getDefaultRolesByDomain() != null ?
                accountConfig.getDefaultRolesByDomain().get(account.getDomain()) :
                null;

        final boolean defaultsAvailable = defaultRoles != null && !defaultRoles.isEmpty();

        if (hasNoRoles && defaultsAvailable) {
            return account.withRoles(defaultRoles);
        }

        return account;
    }
}
