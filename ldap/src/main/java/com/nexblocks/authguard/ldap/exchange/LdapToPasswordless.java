package com.nexblocks.authguard.ldap.exchange;

import com.nexblocks.authguard.basic.passwordless.PasswordlessProvider;
import com.nexblocks.authguard.ldap.LdapService;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.google.inject.Inject;

@TokenExchange(from = "ldap", to = "passwordless")
public class LdapToPasswordless extends LdapExchange implements Exchange {
    @Inject
    public LdapToPasswordless(final LdapService ldapService, final PasswordlessProvider passwordlessProvider) {
        super(ldapService, passwordlessProvider);
    }
}
