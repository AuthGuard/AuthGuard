package com.nexblocks.authguard.basic.passwordless;

import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.model.AccountBO;

public class PasswordlessMessageBody {
    private final AccountTokenDO accountToken;
    private final AccountBO account;

    public PasswordlessMessageBody(final AccountTokenDO accountToken, final AccountBO account) {
        this.accountToken = accountToken;
        this.account = account;
    }

    public AccountTokenDO getAccountToken() {
        return accountToken;
    }

    public AccountBO getAccount() {
        return account;
    }
}
