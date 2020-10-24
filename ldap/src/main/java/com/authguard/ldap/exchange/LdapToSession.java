package com.authguard.ldap.exchange;

import com.authguard.ldap.LdapService;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.sessions.SessionProvider;
import com.google.inject.Inject;

@TokenExchange(from = "ldap", to = "session")
public class LdapToSession extends LdapExchange implements Exchange {
    @Inject
    public LdapToSession(final LdapService ldapService, final SessionProvider sessionProvider) {
        super(ldapService, sessionProvider);
    }
}
