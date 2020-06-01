package com.authguard.service.exchange;

import com.authguard.service.jwt.AccessTokenProvider;
import com.authguard.service.ldap.LdapExchange;
import com.authguard.service.ldap.LdapService;
import com.authguard.service.sessions.SessionProvider;
import com.google.inject.Inject;

@TokenExchange(from = "ldap", to = "accessToken")
public class LdapToAccessToken extends LdapExchange implements Exchange {
    @Inject
    public LdapToAccessToken(final LdapService ldapService, final AccessTokenProvider accessTokenProvider) {
        super(ldapService, accessTokenProvider);
    }
}
