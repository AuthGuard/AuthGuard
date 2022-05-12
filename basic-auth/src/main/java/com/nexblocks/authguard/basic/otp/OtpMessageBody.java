package com.nexblocks.authguard.basic.otp;

import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.OneTimePasswordBO;

public class OtpMessageBody {
    private final OneTimePasswordBO otp;
    private final AccountBO account;
    private final boolean byEmail;
    private final boolean bySms;

    public OtpMessageBody(final OneTimePasswordBO otp, final AccountBO account,
                          final boolean byEmail, final boolean bySms) {
        this.otp = otp;
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
                ", byEmail=" + byEmail +
                ", bySms=" + bySms +
                '}';
    }
}
