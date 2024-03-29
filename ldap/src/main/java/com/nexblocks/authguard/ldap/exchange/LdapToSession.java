package com.nexblocks.authguard.ldap.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.ldap.UnboundLdapService;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.sessions.SessionProvider;

@TokenExchange(from = "ldap", to = "session")
public class LdapToSession extends LdapExchange implements Exchange {
    @Inject
    public LdapToSession(final UnboundLdapService ldapService, final SessionProvider sessionProvider) {
        super(ldapService, sessionProvider);
    }
}
