package com.nexblocks.authguard.basic.passwordless;

import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;

public class PasswordlessMessageBody {
    private final AccountTokenDO accountToken;
    private final AccountBO account;
    private final TokenOptionsBO tokenOptions;

    public PasswordlessMessageBody(final AccountTokenDO accountToken, final AccountBO account,
                                   final TokenOptionsBO tokenOptions) {
        this.accountToken = accountToken;
        this.account = account;
        this.tokenOptions = tokenOptions;
    }

    public AccountTokenDO getAccountToken() {
        return accountToken;
    }

    public AccountBO getAccount() {
        return account;
    }

    public TokenOptionsBO getTokenOptions() {
        return tokenOptions;
    }

    @Override
    public String toString() {
        return "PasswordlessMessageBody{" +
                "accountToken=" + accountToken +
                ", account=" + account +
                ", tokenOptions=" + tokenOptions +
                '}';
    }
}
