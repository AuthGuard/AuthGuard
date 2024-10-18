package com.nexblocks.authguard.basic.totp;

import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;

public class TotpLinkerMessageBody {
    private final String token;
    private final AccountBO account;
    private final TokenOptionsBO tokenOptions;

    public TotpLinkerMessageBody(final String token, final AccountBO account, final TokenOptionsBO tokenOptions) {
        this.token = token;
        this.account = account;
        this.tokenOptions = tokenOptions;
    }

    public String getToken() {
        return token;
    }

    public AccountBO getAccount() {
        return account;
    }

    public TokenOptionsBO getTokenOptions() {
        return tokenOptions;
    }

    @Override
    public String toString() {
        return "TotpLinkerMessageBody{" +
                "token='" + token + '\'' +
                ", account=" + account +
                ", tokenOptions=" + tokenOptions +
                '}';
    }
}
