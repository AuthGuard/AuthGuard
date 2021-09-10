package com.nexblocks.authguard.service.messaging;

import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.model.AccountBO;

public class ResetTokenMessage {
    private final AccountBO account;
    private final AccountTokenDO accountToken;

    public ResetTokenMessage(final AccountBO account, final AccountTokenDO accountToken) {
        this.account = account;
        this.accountToken = accountToken;
    }

    public AccountBO getAccount() {
        return account;
    }

    public AccountTokenDO getAccountToken() {
        return accountToken;
    }
}
