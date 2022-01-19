package com.nexblocks.authguard.ldap.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.passwordless.PasswordlessProvider;
import com.nexblocks.authguard.ldap.UnboundLdapService;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;

@TokenExchange(from = "ldap", to = "passwordless")
public class LdapToPasswordless extends LdapExchange implements Exchange {
    @Inject
    public LdapToPasswordless(final UnboundLdapService ldapService, final PasswordlessProvider passwordlessProvider) {
        super(ldapService, passwordlessProvider);
    }
}
