package com.nexblocks.authguard.basic.otp;

import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.OneTimePasswordBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;

public class OtpMessageBody {
    private final OneTimePasswordBO otp;
    private final AccountBO account;
    private final TokenOptionsBO tokenOptions;
    private final boolean byEmail;
    private final boolean bySms;

    public OtpMessageBody(final OneTimePasswordBO otp, final AccountBO account,
                          final TokenOptionsBO tokenOptions,
                          final boolean byEmail, final boolean bySms) {
        this.otp = otp;
        this.tokenOptions = tokenOptions;
        this.account = account;
        this.byEmail = byEmail;
        this.bySms = bySms;
    }

    public OneTimePasswordBO getOtp() {
        return otp;
    }

    public AccountBO getAccount() {
        return account;
    }

    public TokenOptionsBO getTokenOptions() {
        return tokenOptions;
    }

    public boolean isByEmail() {
        return byEmail;
    }

    public boolean isBySms() {
        return bySms;
    }

    @Override
    public String toString() {
        return "OtpMessageBody{" +
                "otp=" + otp +
                ", account=" + account +
                ", tokenOptions=" + tokenOptions +
                ", byEmail=" + byEmail +
                ", bySms=" + bySms +
                '}';
    }
}
