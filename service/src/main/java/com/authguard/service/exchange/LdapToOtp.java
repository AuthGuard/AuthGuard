package com.authguard.service.exchange;

import com.authguard.ldap.exchange.LdapExchange;
import com.authguard.ldap.LdapService;
import com.authguard.service.otp.OtpProvider;
import com.google.inject.Inject;

@TokenExchange(from = "ldap", to = "otp")
public class LdapToOtp extends LdapExchange implements Exchange {
    @Inject
    public LdapToOtp(final LdapService ldapService, final OtpProvider otpProvider) {
        super(ldapService, otpProvider);
    }
}
