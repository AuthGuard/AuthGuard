package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.jwt.AccessTokenVerifier;
import com.nexblocks.authguard.jwt.crypto.TokenEncryptorAdapter;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import io.smallrye.mutiny.Uni;
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
    public Uni<AuthResponseBO> exchange(final AuthRequestBO request) {
        String encrypted = request.getToken();
        Either<Exception, String> decrypted = tokenEncryptor.decryptEncoded(encrypted);

        if (decrypted.isLeft()) {
            return Uni.createFrom().failure(decrypted.getLeft());
        }

        return accessTokenVerifier.verify(decrypted.get())
                .map(ignored -> AuthResponseBO.builder()
                        .type(TOKEN_TYPE)
                        .token(decrypted.get())
                        .build());
    }
}
