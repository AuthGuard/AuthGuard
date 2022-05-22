package com.nexblocks.authguard.service.util;

import com.nexblocks.authguard.service.config.AccountConfig;
import com.nexblocks.authguard.service.exceptions.ServiceConflictException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.UserIdentifier;
import com.nexblocks.authguard.service.model.UserIdentifierBO;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AccountPreProcessor {
    public static AccountBO preProcess(final AccountBO account, final AccountConfig accountConfig) {
        if (account.getEmail() == null && accountConfig.requireEmail()) {
            throw new ServiceException(ErrorCode.ACCOUNT_EMAIL_REQUIRED, "Account must have an email");
        }

        if (account.getPhoneNumber() == null && accountConfig.requirePhoneNumber()) {
            throw new ServiceException(ErrorCode.ACCOUNT_PHONE_NUMBER_REQUIRED, "Account must have a phone number");
        }

        final AccountBO.Builder builder = AccountBO.builder()
                .from(account);

        addDefaultRolesIfNeeded(builder, account, accountConfig);
        addIdentifiersIfNeeded(builder, account);

        return builder.build();
    }

    private static void addDefaultRolesIfNeeded(final AccountBO.Builder builder,
                                                final AccountBO account,
                                                final AccountConfig accountConfig) {
        final boolean hasNoRoles = account.getRoles() == null || account.getRoles().isEmpty();
        final Set<String> defaultRoles = accountConfig.getDefaultRolesByDomain() != null ?
                accountConfig.getDefaultRolesByDomain().get(account.getDomain()) :
                null;

        final boolean defaultsAvailable = defaultRoles != null && !defaultRoles.isEmpty();

        if (hasNoRoles && defaultsAvailable) {
            builder.roles(defaultRoles);
        }
    }

    private static void addIdentifiersIfNeeded(final AccountBO.Builder builder, final AccountBO account) {
        if (account.getEmail() == null && account.getPhoneNumber() == null) {
            return;
        }

        final List<UserIdentifierBO> combined = new ArrayList<>(account.getIdentifiers());

        if (account.getEmail() != null) {
            combined.add(UserIdentifierBO.builder()
                    .active(true)
                    .domain(account.getDomain())
                    .identifier(account.getEmail().getEmail())
                    .type(UserIdentifier.Type.EMAIL)
                    .build());
        }

        if (account.getPhoneNumber() != null) {
            combined.add(UserIdentifierBO.builder()
                    .active(true)
                    .domain(account.getDomain())
                    .identifier(account.getPhoneNumber().getNumber())
                    .type(UserIdentifier.Type.PHONE_NUMBER)
                    .build());
        }

        builder.identifiers(combined);
    }
}
