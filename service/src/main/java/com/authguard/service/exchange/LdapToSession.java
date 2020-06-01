package com.authguard.service.exchange;

import com.authguard.service.ldap.LdapExchange;
import com.authguard.service.ldap.LdapService;
import com.authguard.service.sessions.SessionProvider;
import com.google.inject.Inject;

@TokenExchange(from = "ldap", to = "session")
public class LdapToSession extends LdapExchange implements Exchange {
    @Inject
    public LdapToSession(final LdapService ldapService, final SessionProvider sessionProvider) {
        super(ldapService, sessionProvider);
    }
}
