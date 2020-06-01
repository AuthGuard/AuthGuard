package com.authguard.service.exchange;

import com.authguard.service.ldap.LdapExchange;
import com.authguard.service.ldap.LdapService;
import com.authguard.service.passwordless.PasswordlessProvider;
import com.google.inject.Inject;

@TokenExchange(from = "ldap", to = "passwordless")
public class LdapToPasswordless extends LdapExchange implements Exchange {
    @Inject
    public LdapToPasswordless(final LdapService ldapService, final PasswordlessProvider passwordlessProvider) {
        super(ldapService, passwordlessProvider);
    }
}
