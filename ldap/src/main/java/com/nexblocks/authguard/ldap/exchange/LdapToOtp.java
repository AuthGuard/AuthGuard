package com.nexblocks.authguard.ldap.exchange;

import com.nexblocks.authguard.basic.otp.OtpProvider;
import com.nexblocks.authguard.ldap.LdapService;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.google.inject.Inject;

@TokenExchange(from = "ldap", to = "otp")
public class LdapToOtp extends LdapExchange implements Exchange {
    @Inject
    public LdapToOtp(final LdapService ldapService, final OtpProvider otpProvider) {
        super(ldapService, otpProvider);
    }
}
