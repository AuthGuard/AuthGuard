package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.jwt.AccessTokenVerifier;
import com.nexblocks.authguard.jwt.crypto.TokenEncryptorAdapter;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import io.vavr.control.Either;

@TokenExchange(from = "encryptedToken", to = "accessToken")
public class EncryptedToAccessToken implements Exchange {
    private static final String TOKEN_TYPE = "accessToken";

    private final AccessTokenVerifier accessTokenVerifier;
    private final TokenEncryptorAdapter tokenEncryptor;

    @Inject
    public EncryptedToAccessToken(final AccessTokenVerifier accessTokenVerifier, final TokenEncryptorAdapter tokenEncryptor) {
        this.accessTokenVerifier = accessTokenVerifier;
        this.tokenEncryptor = tokenEncryptor;
    }

    @Override
    public Either<Exception, AuthResponseBO> exchange(final AuthRequestBO request) {
        final String encrypted = request.getToken();
        return tokenEncryptor.decryptEncoded(encrypted)
                .flatMap(accessTokenVerifier::verify)
                .map(token -> AuthResponseBO.builder()
                        .type(TOKEN_TYPE)
                        .token(token)
                        .build());
    }
}
