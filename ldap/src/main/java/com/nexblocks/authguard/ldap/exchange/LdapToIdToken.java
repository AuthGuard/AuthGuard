package com.nexblocks.authguard.ldap.exchange;

import com.nexblocks.authguard.jwt.IdTokenProvider;
import com.nexblocks.authguard.ldap.LdapService;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.google.inject.Inject;

@TokenExchange(from = "ldap", to = "idToken")
public class LdapToIdToken extends LdapExchange implements Exchange {
    @Inject
    public LdapToIdToken(final LdapService ldapService, final IdTokenProvider idTokenProvider) {
        super(ldapService, idTokenProvider);
    }
}
