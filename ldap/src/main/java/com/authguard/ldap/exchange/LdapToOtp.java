package com.authguard.ldap.exchange;

import com.authguard.basic.otp.OtpProvider;
import com.authguard.ldap.LdapService;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.google.inject.Inject;

@TokenExchange(from = "ldap", to = "otp")
public class LdapToOtp extends LdapExchange implements Exchange {
    @Inject
    public LdapToOtp(final LdapService ldapService, final OtpProvider otpProvider) {
        super(ldapService, otpProvider);
    }
}
