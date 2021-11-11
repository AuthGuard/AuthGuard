package com.nexblocks.authguard.ldap.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.otp.OtpProvider;
import com.nexblocks.authguard.ldap.UnboundLdapService;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;

@TokenExchange(from = "ldap", to = "otp")
public class LdapToOtp extends LdapExchange implements Exchange {
    @Inject
    public LdapToOtp(final UnboundLdapService ldapService, final OtpProvider otpProvider) {
        super(ldapService, otpProvider);
    }
}
