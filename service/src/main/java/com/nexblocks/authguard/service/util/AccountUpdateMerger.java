package com.nexblocks.authguard.service.util;

import com.nexblocks.authguard.service.model.AccountBO;

public class AccountUpdateMerger {
    public static AccountBO merge(final AccountBO existing, final AccountBO update) {
        final AccountBO.Builder merged = AccountBO.builder().from(existing);

        if (update.getFirstName() != null) {
            merged.firstName(update.getFirstName());
        }

        if (update.getMiddleName() != null) {
            merged.middleName(update.getMiddleName());
        }

        if (update.getLastName() != null) {
            merged.lastName(update.getLastName());
        }

        if (update.getEmail() != null) {
            merged.email(update.getEmail());
        }

        if (update.getBackupEmail() != null) {
            merged.backupEmail(update.getBackupEmail());
        }

        if (update.getPhoneNumber() != null) {
            merged.phoneNumber(update.getPhoneNumber());
        }

        return merged.build();
    }
}
