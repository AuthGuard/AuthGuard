package com.nexblocks.authguard.ldap.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.jwt.oauth.AuthorizationCodeProvider;
import com.nexblocks.authguard.ldap.UnboundLdapService;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;

@TokenExchange(from = "ldap", to = "authorizationCode")
public class LdapToAuthorizationCode extends LdapExchange implements Exchange {
    @Inject
    public LdapToAuthorizationCode(final UnboundLdapService ldapService,
                                   final AuthorizationCodeProvider authorizationCodeProvider) {
        super(ldapService, authorizationCodeProvider);
    }
}
