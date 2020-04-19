package com.authguard.service.exchange;

import com.authguard.service.AccountsService;
import com.authguard.service.jwt.AccessTokenProvider;
import com.authguard.service.model.TokensBO;
import com.authguard.service.oauth.AuthorizationCodeVerifier;
import com.google.inject.Inject;

import java.util.Optional;

@TokenExchange(from = "authorizationCode", to = "accessToken")
public class AuthorizationCodeToAccessToken implements Exchange {
    private final AccountsService accountsService;
    private final AuthorizationCodeVerifier authorizationCodeVerifier;
    private final AccessTokenProvider accessTokenProvider;

    @Inject
    public AuthorizationCodeToAccessToken(final AccountsService accountsService,
                                          final AuthorizationCodeVerifier authorizationCodeVerifier,
                                          final AccessTokenProvider accessTokenProvider) {
        this.accountsService = accountsService;
        this.authorizationCodeVerifier = authorizationCodeVerifier;
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String authorizationCode) {
        return authorizationCodeVerifier.verifyAccountToken(authorizationCode)
                .flatMap(accountsService::getById)
                .map(accessTokenProvider::generateToken);
    }
}
