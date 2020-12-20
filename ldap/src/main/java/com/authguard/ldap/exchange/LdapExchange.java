package com.authguard.ldap.exchange;

import com.authguard.ldap.LdapService;
import com.authguard.service.auth.AuthProvider;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.TokensBO;
import io.vavr.control.Either;

import java.util.Base64;

public abstract class LdapExchange implements Exchange {
    private final LdapService ldapService;
    private final AuthProvider authProvider;

    protected LdapExchange(final LdapService ldapService, final AuthProvider authProvider) {
        this.ldapService = ldapService;
        this.authProvider = authProvider;
    }

    @Override
    public Either<Exception, TokensBO> exchange(final AuthRequestBO request) {
        final AccountBO account = ldapService.authenticate(request.getIdentifier(), request.getPassword());

        return Either.right(authProvider.generateToken(account));
    }

    public static String[] decodeAndSplitBasic(final String base64) {
        return new String(Base64.getDecoder().decode(base64)).split(":");
    }
}
