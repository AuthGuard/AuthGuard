package com.nexblocks.authguard.ldap.exchange;

import com.nexblocks.authguard.jwt.AccessTokenProvider;
import com.nexblocks.authguard.ldap.LdapService;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.google.inject.Inject;

@TokenExchange(from = "ldap", to = "accessToken")
public class LdapToAccessToken extends LdapExchange implements Exchange {
    @Inject
    public LdapToAccessToken(final LdapService ldapService, final AccessTokenProvider accessTokenProvider) {
        super(ldapService, accessTokenProvider);
    }
}
