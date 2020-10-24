package com.authguard.ldap.exchange;

import com.authguard.basic.passwordless.PasswordlessProvider;
import com.authguard.ldap.LdapService;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.google.inject.Inject;

@TokenExchange(from = "ldap", to = "passwordless")
public class LdapToPasswordless extends LdapExchange implements Exchange {
    @Inject
    public LdapToPasswordless(final LdapService ldapService, final PasswordlessProvider passwordlessProvider) {
        super(ldapService, passwordlessProvider);
    }
}
