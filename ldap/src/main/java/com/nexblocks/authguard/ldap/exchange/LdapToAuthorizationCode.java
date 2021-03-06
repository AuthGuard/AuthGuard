package com.nexblocks.authguard.ldap.exchange;

import com.nexblocks.authguard.jwt.oauth.AuthorizationCodeProvider;
import com.nexblocks.authguard.ldap.LdapService;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.google.inject.Inject;

@TokenExchange(from = "ldap", to = "authorizationCode")
public class LdapToAuthorizationCode extends LdapExchange implements Exchange {
    @Inject
    public LdapToAuthorizationCode(final LdapService ldapService,
                                   final AuthorizationCodeProvider authorizationCodeProvider) {
        super(ldapService, authorizationCodeProvider);
    }
}
